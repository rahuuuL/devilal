package com.terminal_devilal.decision.indicator;

import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.service.MannKendallHistoryService;
import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class MannKendallSourceProvider implements IndicatorProvider {
    private static final Logger log = LoggerFactory.getLogger(MannKendallSourceProvider.class);
    private final MannKendallHistoryService historyService;
    private final DecisionIndicatorRepository indicatorRepository;
    private final SubjectTickerResolver tickerResolver;
    private final ExpressionParser parser = new SpelExpressionParser();

    public MannKendallSourceProvider(MannKendallHistoryService historyService, DecisionIndicatorRepository indicatorRepository, SubjectTickerResolver tickerResolver) {
        this.historyService = historyService;
        this.indicatorRepository = indicatorRepository;
        this.tickerResolver = tickerResolver;
    }

    @Override
    public String getIndicatorCode() {
        return "MK_SOURCE";
    }

    @Override
    public Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters) {
        log.debug("MannKendallSourceProvider.getValue() called. Context: subjectType={}, subjectId={}, asOfDate={}", 
                context != null ? context.getSubjectType() : "null",
                context != null ? context.getSubjectId() : "null", 
                context != null ? context.getAsOfDate() : "null");
        
        if (context == null || context.getAsOfDate() == null) {
            log.warn("MannKendallSourceProvider: context or asOfDate is null, returning null");
            return null;
        }
        
        // Note: For MARKET type, subjectId can be null/empty - that's OK, we'll resolve all market tickers
        if (context.getSubjectType() == null || context.getSubjectType().isBlank()) {
            log.warn("MannKendallSourceProvider: subjectType is null or empty, returning null");
            return null;
        }

        String indicatorCode = parameters == null || parameters.isEmpty() ? "MK_SCORE" :
                (String) parameters.getOrDefault("indicatorCode", "MK_SCORE");
        DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
        if (indicator == null || indicator.getSourceProviderCode() == null || indicator.getFieldExpression() == null) {
            log.warn("MannKendallSourceProvider: Indicator {} not found or missing config (sourceProviderCode or fieldExpression)", indicatorCode);
            return null;
        }

        LocalDate toDate = parameters != null && parameters.get("toDate") instanceof LocalDate localDate ? localDate : context.getAsOfDate();
        LocalDate fromDate = parameters != null && parameters.get("fromDate") instanceof LocalDate localDate ? localDate : toDate;
        Integer days = parameters != null && parameters.get("days") instanceof Number number ? number.intValue() : null;
        if (days == null || toDate == null) {
            log.warn("MannKendallSourceProvider: days or toDate is null, returning null");
            return null;
        }

        List<String> tickers = resolveTickers(context, tickerResolver);
        log.info("MannKendallSourceProvider: Resolved tickers for indicator {}. Count: {}, Tickers: {}", indicatorCode, tickers.size(), tickers);
        
        if (tickers.isEmpty()) {
            log.warn("MannKendallSourceProvider: No tickers resolved for indicator {}", indicatorCode);
            return null;
        }
        
        List<MkResultHistoryEntity> rows = historyService.fetchByDateRangeDaysAndTickers(fromDate, toDate, days, Set.copyOf(tickers));
        log.debug("MannKendallSourceProvider: Fetched {} rows for indicator {} from {} to {} with {} days", rows.size(), indicatorCode, fromDate, toDate, days);
        
        if (rows.isEmpty()) {
            log.warn("MannKendallSourceProvider: No rows found for indicator {}", indicatorCode);
            return null;
        }

        String aggregation = indicator.getRowAggregation() == null ? "SINGLE" : indicator.getRowAggregation();
        String fieldExpression = indicator.getFieldExpression();
        List<Object> values = rows.stream()
                .filter(row -> row != null && tickers.contains(row.getTicker()))
                .filter(row -> row.getDate() != null && row.getDate().isEqual(toDate))
                .map(row -> parser.parseExpression(fieldExpression).getValue(row))
                .filter(Objects::nonNull)
                .toList();

        log.info("MannKendallSourceProvider: Filtered to {} values for indicator {} with date {} and tickers {}", 
                values.size(), indicatorCode, toDate, tickers);

        if (values.isEmpty()) {
            log.warn("MannKendallSourceProvider: No values extracted for indicator {} using fieldExpression: {}", indicatorCode, fieldExpression);
            return null;
        }

        Object result = switch (aggregation.toUpperCase()) {
            case "MAX" -> values.stream().mapToDouble(this::toDoubleOrNull).max().orElse(Double.NaN);
            case "MIN" -> values.stream().mapToDouble(this::toDoubleOrNull).min().orElse(Double.NaN);
            case "FIRST" -> values.get(0);
            case "SINGLE" -> values.get(0);
            default -> values.get(0);
        };
        log.info("MannKendallSourceProvider: Returning {} for indicator {} using aggregation {}", result, indicatorCode, aggregation);
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
