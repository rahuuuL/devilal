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
import com.terminal_devilal.indicators.vwap.entity.projections.VwapProjection;
import com.terminal_devilal.indicators.vwap.service.VWAPService;

@Component
public class VwapSourceProvider implements IndicatorProvider {

    private final VWAPService vwapService;
    private final DecisionIndicatorRepository indicatorRepository;
    private final ExpressionParser parser = new SpelExpressionParser();

    public VwapSourceProvider(VWAPService vwapService, DecisionIndicatorRepository indicatorRepository) {
        this.vwapService = vwapService;
        this.indicatorRepository = indicatorRepository;
    }

    @Override
    public String getIndicatorCode() {
        return "VWAP_SOURCE";
    }

    @Override
    public Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters) {
        if (context == null || context.getSubjectId() == null || context.getAsOfDate() == null) {
            return null;
        }

        String indicatorCode = parameters == null || parameters.isEmpty() ? "VWAP"
                : (String) parameters.getOrDefault("indicatorCode", "VWAP");
        DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
        if (indicator == null || indicator.getFieldExpression() == null) {
            return null;
        }

        LocalDate toDate = parameters != null && parameters.get("toDate") instanceof LocalDate localDate ? localDate : context.getAsOfDate();
        LocalDate fromDate = parameters != null && parameters.get("fromDate") instanceof LocalDate localDate ? localDate : toDate.minusMonths(18);

        List<VwapProjection> rows = vwapService.getVwapDataWithinDates(List.of(context.getSubjectId()), fromDate, toDate);
        if (rows.isEmpty()) {
            return null;
        }

        String fieldExpression = indicator.getFieldExpression();
        String aggregation = indicator.getRowAggregation() == null ? "SINGLE" : indicator.getRowAggregation();
        List<Object> values = rows.stream()
                .filter(row -> row != null && Objects.equals(row.getTicker(), context.getSubjectId()))
                .filter(row -> row.getDate() != null && row.getDate().isEqual(toDate))
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