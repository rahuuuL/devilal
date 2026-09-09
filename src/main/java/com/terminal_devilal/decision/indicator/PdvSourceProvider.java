package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;

@Component
public class PdvSourceProvider implements IndicatorProvider {

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
        if (context == null || context.getSubjectId() == null || context.getAsOfDate() == null) {
            return null;
        }

        String indicatorCode = parameters == null || parameters.isEmpty() ? "DELIVERY_PERCENT"
                : (String) parameters.getOrDefault("indicatorCode", "DELIVERY_PERCENT");
        DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
        if (indicator == null || indicator.getFieldExpression() == null) {
            return null;
        }

        LocalDate toDate = parameters != null && parameters.get("toDate") instanceof LocalDate localDate ? localDate : context.getAsOfDate();
        LocalDate fromDate = parameters != null && parameters.get("fromDate") instanceof LocalDate localDate ? localDate : toDate.minusMonths(18);

        Map<String, List<PriceDeliveryVolumeEntity>> rowsByTicker = service.getPDVForTickerSince(fromDate, List.of(context.getSubjectId()));
        List<PriceDeliveryVolumeEntity> rows = rowsByTicker.getOrDefault(context.getSubjectId(), List.of());
        if (rows.isEmpty()) {
            return null;
        }

        String fieldExpression = indicator.getFieldExpression();
        String aggregation = indicator.getRowAggregation() == null ? "SINGLE" : indicator.getRowAggregation();
        List<Object> values = rows.stream()
                .filter(row -> row != null && row.getDate() != null && row.getDate().isEqual(toDate))
                .map(row -> parser.parseExpression(fieldExpression).getValue(row))
                .filter(Objects::nonNull)
                .toList();

        if (values.isEmpty()) {
            return null;
        }

        return switch (aggregation.toUpperCase()) {
            case "MAX" -> values.stream().mapToDouble(this::toDoubleOrNull).max().orElse(Double.NaN);
            case "MIN" -> values.stream().mapToDouble(this::toDoubleOrNull).min().orElse(Double.NaN);
            case "FIRST" -> values.get(0);
            case "SINGLE" -> values.get(0);
            default -> values.get(0);
        };
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