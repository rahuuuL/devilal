package com.terminal_devilal.decision.indicator;

import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.service.MannKendallHistoryService;
import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
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
    private final MannKendallHistoryService historyService;
    private final DecisionIndicatorRepository indicatorRepository;
    private final ExpressionParser parser = new SpelExpressionParser();

    public MannKendallSourceProvider(MannKendallHistoryService historyService, DecisionIndicatorRepository indicatorRepository) {
        this.historyService = historyService;
        this.indicatorRepository = indicatorRepository;
    }

    @Override
    public String getIndicatorCode() {
        return "MK_SOURCE";
    }

    @Override
    public Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters) {
        if (context == null || context.getSubjectId() == null || context.getAsOfDate() == null) {
            return null;
        }

        String indicatorCode = parameters == null || parameters.isEmpty() ? "MK_SCORE" :
                (String) parameters.getOrDefault("indicatorCode", "MK_SCORE");
        DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
        if (indicator == null || indicator.getSourceProviderCode() == null || indicator.getFieldExpression() == null) {
            return null;
        }

        LocalDate toDate = parameters != null && parameters.get("toDate") instanceof LocalDate localDate ? localDate : context.getAsOfDate();
        LocalDate fromDate = parameters != null && parameters.get("fromDate") instanceof LocalDate localDate ? localDate : toDate;
        Integer days = parameters != null && parameters.get("days") instanceof Number number ? number.intValue() : null;
        if (days == null || toDate == null) {
            return null;
        }

        List<MkResultHistoryEntity> rows = historyService.fetchByDateRangeDaysAndTickers(fromDate, toDate, days, Set.of(context.getSubjectId()));
        if (rows.isEmpty()) {
            return null;
        }

        String aggregation = indicator.getRowAggregation() == null ? "SINGLE" : indicator.getRowAggregation();
        String fieldExpression = indicator.getFieldExpression();
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
