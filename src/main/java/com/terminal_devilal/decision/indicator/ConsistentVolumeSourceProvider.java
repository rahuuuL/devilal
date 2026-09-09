package com.terminal_devilal.decision.indicator;

import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
import com.terminal_devilal.indicators.volume.model.ConsistentVolumeSignalResponse;
import com.terminal_devilal.indicators.volume.service.ConsistentVolumeDetector;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ConsistentVolumeSourceProvider implements IndicatorProvider {
    private final DecisionIndicatorRepository indicatorRepository;
    private final ConsistentVolumeDetector detector;
    private final ExpressionParser parser = new SpelExpressionParser();

    public ConsistentVolumeSourceProvider(DecisionIndicatorRepository indicatorRepository, ConsistentVolumeDetector detector) {
        this.indicatorRepository = indicatorRepository;
        this.detector = detector;
    }

    @Override
    public String getIndicatorCode() {
        return "CONSISTENT_VOLUME_SOURCE";
    }

    @Override
    public Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters) {
        if (context == null || context.getSubjectId() == null || context.getAsOfDate() == null) {
            return null;
        }

        String indicatorCode = parameters == null || parameters.isEmpty() ? "CONSISTENT_VOLUME_SCORE"
                : (String) parameters.getOrDefault("indicatorCode", "CONSISTENT_VOLUME_SCORE");
        DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
        if (indicator == null || indicator.getSourceProviderCode() == null || indicator.getFieldExpression() == null) {
            return null;
        }

        LocalDate fromDate = parameters != null && parameters.get("fromDate") instanceof LocalDate localDate ? localDate : context.getAsOfDate().minusMonths(18);
        LocalDate toDate = parameters != null && parameters.get("toDate") instanceof LocalDate localDate ? localDate : context.getAsOfDate();

        List<String> tickers = List.of(context.getSubjectId().split(ConsistentVolumeDetector.TICKER_SEPARATOR));
        List<ConsistentVolumeSignalResponse> rows = detector.detectConsistentVolumes(
                tickers,
                fromDate,
                toDate,
                asInt(parameters != null ? parameters.get("baselineWindow") : null, 20),
                asDouble(parameters != null ? parameters.get("baselineLowPercentile") : null, 20.0),
                asDouble(parameters != null ? parameters.get("baselineHighPercentile") : null, 80.0),
                asInt(parameters != null ? parameters.get("rvolPercentileWindow") : null, 60),
                asDouble(parameters != null ? parameters.get("rvolThresholdPercentile") : null, 75.0),
                asInt(parameters != null ? parameters.get("consistencyWindow") : null, 10),
                asInt(parameters != null ? parameters.get("requiredScore") : null, 7));

        String fieldExpression = indicator.getFieldExpression();
        List<ConsistentVolumeSignalResponse> subjectRows = rows.stream()
                .filter(row -> row != null && Objects.equals(row.getTicker(), context.getSubjectId()))
                .filter(row -> row.getDate() != null && row.getDate().isEqual(toDate))
                .toList();

        if (subjectRows.isEmpty()) {
            return null;
        }

        List<Object> values = subjectRows.stream()
                .map(row -> parser.parseExpression(fieldExpression).getValue(row))
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return null;
        }

        String aggregation = indicator.getRowAggregation() == null ? "SINGLE" : indicator.getRowAggregation();
        return switch (aggregation.toUpperCase()) {
            case "MAX" -> values.stream().mapToDouble(this::toDoubleOrNull).max().orElse(Double.NaN);
            case "MIN" -> values.stream().mapToDouble(this::toDoubleOrNull).min().orElse(Double.NaN);
            case "FIRST" -> values.get(0);
            case "SINGLE" -> values.get(0);
            default -> values.get(0);
        };
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        return fallback;
    }

    private double asDouble(Object value, double fallback) {
        if (value instanceof Number number) return number.doubleValue();
        return fallback;
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
