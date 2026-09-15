package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    public PvppSourceProvider(PvppHistoryService historyService, DecisionIndicatorRepository indicatorRepository) {
        this.historyService = historyService;
        this.indicatorRepository = indicatorRepository;
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

        List<String> tickers;
        if ("TICKER".equalsIgnoreCase(context.getSubjectType())) {
            if (context.getSubjectId() == null || context.getSubjectId().isBlank()) {
                return null;
            }
            tickers = List.of(context.getSubjectId());
        } else if ("MARKET".equalsIgnoreCase(context.getSubjectType())) {
            Object tickerParameter = parameters == null ? null : parameters.get("tickers");
            if (!(tickerParameter instanceof Collection<?> collection)) {
                throw new IllegalArgumentException("MARKET PVPP provider requires parameters.tickers");
            }
            tickers = collection.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        } else {
            throw new IllegalArgumentException("Unsupported subject type: " + context.getSubjectType());
        }
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
        Map<String, List<Object>> valuesByTicker = new LinkedHashMap<>();
        for (PvppResultHistoryResponse row : rows) {
            if (row == null || row.getTicker() == null || row.getDate() == null || !row.getDate().isEqual(toDate)) {
                continue;
            }
            Object value = parser.parseExpression(fieldExpression).getValue(row);
            if (value != null) {
                valuesByTicker.computeIfAbsent(row.getTicker(), ignored -> new ArrayList<>()).add(value);
            }
        }
        if ("TICKER".equalsIgnoreCase(context.getSubjectType())) {
            return aggregateValues(valuesByTicker.get(tickers.get(0)), aggregation);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        valuesByTicker.forEach((ticker, values) -> {
            Object value = aggregateValues(values, aggregation);
            if (value != null) {
                result.put(ticker, value);
            }
        });
        return result;
    }

    private Object aggregateValues(List<Object> values, String aggregation) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return switch (aggregation.toUpperCase(Locale.ROOT)) {
            case "MAX" -> values.stream().map(this::toDoubleOrNull).filter(Objects::nonNull).max(Double::compareTo).orElse(null);
            case "MIN" -> values.stream().map(this::toDoubleOrNull).filter(Objects::nonNull).min(Double::compareTo).orElse(null);
            case "FIRST", "SINGLE" -> values.get(0);
            default -> values.get(0);
        };
    }

    private Double toDoubleOrNull(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            return Double.parseDouble(text);
        }
        return null;
    }
}