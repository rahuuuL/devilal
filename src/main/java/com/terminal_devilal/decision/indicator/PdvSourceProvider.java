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
    private final SubjectTickerResolver tickerResolver;

    public PdvSourceProvider(PriceDeliveryVolumeService service, DecisionIndicatorRepository indicatorRepository, SubjectTickerResolver tickerResolver) {
        this.service = service;
        this.indicatorRepository = indicatorRepository;
        this.tickerResolver = tickerResolver;
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

        List<String> tickers = resolveTickers(context, tickerResolver);
        log.info("PdvSourceProvider: Resolved tickers for indicator {}. Count: {}, Tickers: {}", indicatorCode, tickers.size(), tickers);
        
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
        List<Object> values = rows.stream()
                .filter(row -> row != null && row.getDate() != null && row.getDate().isEqual(toDate))
                .map(row -> parser.parseExpression(fieldExpression).getValue(row))
                .filter(Objects::nonNull)
                .toList();

        log.info("PdvSourceProvider: Filtered to {} values for indicator {} with date {}", values.size(), indicatorCode, toDate);

        if (values.isEmpty()) {
            log.warn("PdvSourceProvider: No values extracted for indicator {} using fieldExpression: {}", indicatorCode, fieldExpression);
            return null;
        }

        Object result = switch (aggregation.toUpperCase()) {
            case "MAX" -> values.stream().mapToDouble(this::toDoubleOrNull).max().orElse(Double.NaN);
            case "MIN" -> values.stream().mapToDouble(this::toDoubleOrNull).min().orElse(Double.NaN);
            case "FIRST" -> values.get(0);
            case "SINGLE" -> values.get(0);
            default -> values.get(0);
        };
        log.info("PdvSourceProvider: Returning {} for indicator {} using aggregation {}", result, indicatorCode, aggregation);
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