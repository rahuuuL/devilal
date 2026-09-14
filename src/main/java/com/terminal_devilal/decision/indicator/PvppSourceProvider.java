package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import com.terminal_devilal.business_tools.pvpp.dto.PvppResultHistoryResponse;
import com.terminal_devilal.business_tools.pvpp.service.PvppHistoryService;
import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;

@Component
public class PvppSourceProvider implements IndicatorProvider {
    private static final Logger log = LoggerFactory.getLogger(PvppSourceProvider.class);
    private final PvppHistoryService historyService;
    private final DecisionIndicatorRepository indicatorRepository;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final SubjectTickerResolver tickerResolver;

    public PvppSourceProvider(PvppHistoryService historyService, DecisionIndicatorRepository indicatorRepository, SubjectTickerResolver tickerResolver) {
        this.historyService = historyService;
        this.indicatorRepository = indicatorRepository;
        this.tickerResolver = tickerResolver;
    }

    @Override
    public String getIndicatorCode() {
        return "PVPP_SOURCE";
    }

    @Override
    public Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters) {
        log.debug("PvppSourceProvider.getValue() called. Context: subjectType={}, subjectId={}, asOfDate={}", 
                context != null ? context.getSubjectType() : "null",
                context != null ? context.getSubjectId() : "null", 
                context != null ? context.getAsOfDate() : "null");
        
        if (context == null || context.getAsOfDate() == null) {
            log.warn("PvppSourceProvider: context or asOfDate is null, returning null");
            return null;
        }
        
        // Note: For MARKET type, subjectId can be null/empty - that's OK, we'll resolve all market tickers
        if (context.getSubjectType() == null || context.getSubjectType().isBlank()) {
            log.warn("PvppSourceProvider: subjectType is null or empty, returning null");
            return null;
        }

        String indicatorCode = parameters == null || parameters.isEmpty() ? "PVPP"
                : (String) parameters.getOrDefault("indicatorCode", "PVPP");
        DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
        if (indicator == null || indicator.getFieldExpression() == null) {
            log.warn("PvppSourceProvider: Indicator {} not found or missing fieldExpression", indicatorCode);
            return null;
        }

        LocalDate toDate = parameters != null && parameters.get("toDate") instanceof LocalDate localDate ? localDate : context.getAsOfDate();
        LocalDate fromDate = parameters != null && parameters.get("fromDate") instanceof LocalDate localDate ? localDate : toDate.minusMonths(18);
        Integer days = parameters != null && parameters.get("days") instanceof Number number ? number.intValue() : null;
        if (days == null) {
            Object value = parameters != null ? parameters.get("window") : null;
            if (value instanceof Number number) {
                days = number.intValue();
            }
        }
        if (days == null || days <= 0) {
            days = 20;
        }

        List<String> tickers = resolveTickers(context, tickerResolver);
        log.info("PvppSourceProvider: Resolved tickers for indicator {}. Count: {}, Tickers: {}", indicatorCode, tickers.size(), tickers);
        
        if (tickers.isEmpty()) {
            log.warn("PvppSourceProvider: No tickers resolved for indicator {}", indicatorCode);
            return null;
        }
        
        List<PvppResultHistoryResponse> rows = historyService.getHistory(fromDate, toDate, days, tickers);
        log.debug("PvppSourceProvider: Fetched {} rows for indicator {} from {} to {} with {} days", rows.size(), indicatorCode, fromDate, toDate, days);
        
        if (rows.isEmpty()) {
            log.warn("PvppSourceProvider: No rows found for indicator {}", indicatorCode);
            return null;
        }

        String fieldExpression = indicator.getFieldExpression();
        String aggregation = indicator.getRowAggregation() == null ? "SINGLE" : indicator.getRowAggregation();
        List<Object> values = rows.stream()
                .filter(row -> row != null && tickers.contains(row.getTicker()))
                .filter(row -> row.getDate() != null && row.getDate().isEqual(toDate))
                .map(row -> parser.parseExpression(fieldExpression).getValue(row))
                .filter(Objects::nonNull)
                .toList();

        log.info("PvppSourceProvider: Filtered to {} values for indicator {} with date {} and tickers {}", 
                values.size(), indicatorCode, toDate, tickers);

        if (values.isEmpty()) {
            log.warn("PvppSourceProvider: No values extracted for indicator {} using fieldExpression: {}", indicatorCode, fieldExpression);
            return null;
        }

        Object result = switch (aggregation.toUpperCase()) {
            case "MAX" -> values.stream().mapToDouble(this::toDoubleOrNull).max().orElse(Double.NaN);
            case "MIN" -> values.stream().mapToDouble(this::toDoubleOrNull).min().orElse(Double.NaN);
            case "FIRST" -> values.get(0);
            case "SINGLE" -> values.get(0);
            default -> values.get(0);
        };
        log.info("PvppSourceProvider: Returning {} for indicator {} using aggregation {}", result, indicatorCode, aggregation);
        return result;
    }

    private double toDoubleOrNull(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            return Double.parseDouble(text);
        }
        return Double.NaN;
    }
}