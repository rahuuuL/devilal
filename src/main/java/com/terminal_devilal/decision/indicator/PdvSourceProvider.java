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

import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;

@Component
public class PdvSourceProvider implements IndicatorProvider {
    private static final Logger log = LoggerFactory.getLogger(PdvSourceProvider.class);
    private final PriceDeliveryVolumeService service;
    private final DecisionIndicatorRepository indicatorRepository;
    private final ExpressionParser parser = new SpelExpressionParser();

    public PdvSourceProvider(PriceDeliveryVolumeService service, DecisionIndicatorRepository indicatorRepository) {
        this.service = service;
        this.indicatorRepository = indicatorRepository;
    }

    @Override
    public String getIndicatorCode() {
        return "PDV_SOURCE";
    }

    @Override
    public Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters) {
        log.debug("PdvSourceProvider.getValue() called. Context: subjectType={}, subjectId={}, asOfDate={}", 
                context != null ? context.getSubjectType() : "null",
                context != null ? context.getSubjectId() : "null", 
                context != null ? context.getAsOfDate() : "null");
        
        if (context == null || context.getAsOfDate() == null) {
            log.warn("PdvSourceProvider: context or asOfDate is null, returning null");
            return null;
        }
        
        // Note: For MARKET type, subjectId can be null/empty - that's OK, we'll resolve all market tickers
        if (context.getSubjectType() == null || context.getSubjectType().isBlank()) {
            log.warn("PdvSourceProvider: subjectType is null or empty, returning null");
            return null;
        }

        String indicatorCode = parameters == null || parameters.isEmpty() ? "DELIVERY_PERCENT"
                : (String) parameters.getOrDefault("indicatorCode", "DELIVERY_PERCENT");
        DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
        if (indicator == null || indicator.getFieldExpression() == null) {
            log.warn("PdvSourceProvider: Indicator {} not found or missing fieldExpression", indicatorCode);
            return null;
        }

        LocalDate toDate = parameters != null && parameters.get("toDate") instanceof LocalDate localDate ? localDate : context.getAsOfDate();
        LocalDate fromDate = parameters != null && parameters.get("fromDate") instanceof LocalDate localDate ? localDate : toDate.minusMonths(18);

        List<String> tickers;
        if ("TICKER".equalsIgnoreCase(context.getSubjectType())) {
            if (context.getSubjectId() == null || context.getSubjectId().isBlank()) {
                return null;
            }
            tickers = List.of(context.getSubjectId());
        } else if ("MARKET".equalsIgnoreCase(context.getSubjectType())) {
            Object tickerParameter = parameters == null ? null : parameters.get("tickers");
            if (!(tickerParameter instanceof Collection<?> collection)) {
                throw new IllegalArgumentException("MARKET PDV provider requires parameters.tickers");
            }
            tickers = collection.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        } else {
            throw new IllegalArgumentException("Unsupported subject type: " + context.getSubjectType());
        }

        if (tickers.isEmpty()) {
            log.warn("PdvSourceProvider: No tickers resolved for indicator {}", indicatorCode);
            return null;
        }

        Map<String, List<PriceDeliveryVolumeEntity>> rowsByTicker = service.getPDVForTickerSince(fromDate, tickers);
        List<PriceDeliveryVolumeEntity> rows = tickers.stream().flatMap(ticker -> rowsByTicker.getOrDefault(ticker, List.of()).stream()).toList();
        log.debug("PdvSourceProvider: Fetched {} rows for indicator {} from {}", rows.size(), indicatorCode, fromDate);

        if (rows.isEmpty()) {
            log.warn("PdvSourceProvider: No rows found for indicator {}", indicatorCode);
            return null;
        }

        String fieldExpression = indicator.getFieldExpression();
        String aggregation = indicator.getRowAggregation() == null ? "SINGLE" : indicator.getRowAggregation();
        Map<String, List<Object>> valuesByTicker = new LinkedHashMap<>();
        for (PriceDeliveryVolumeEntity row : rows) {
            if (row == null || row.getDate() == null || !row.getDate().isEqual(toDate)) {
                continue;
            }
            Object value = parser.parseExpression(fieldExpression).getValue(row);
            if (value != null) {
                valuesByTicker.computeIfAbsent(row.getTicker(), ignored -> new ArrayList<>()).add(value);
            }
        }

        if ("TICKER".equalsIgnoreCase(context.getSubjectType())) {
            return aggregateValues(valuesByTicker.getOrDefault(tickers.get(0), List.of()), aggregation);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        valuesByTicker.forEach((ticker, values) -> {
            Object aggregated = aggregateValues(values, aggregation);
            if (aggregated != null) {
                result.put(ticker, aggregated);
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