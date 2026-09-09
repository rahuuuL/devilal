package com.terminal_devilal.decision.indicator;

import com.terminal_devilal.decision.model.RuleDefinition;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DefaultIndicatorParameterResolver implements IndicatorParameterResolver {

    @Override
    public Map<String, Object> resolve(String indicatorCode, RuleDefinition.Condition condition, IndicatorEvaluationContext context) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (condition != null && condition.parameters() != null) {
            params.putAll(condition.parameters());
        }

        if (context == null || context.getAsOfDate() == null) {
            throw new IllegalArgumentException("Indicator evaluation requires asOfDate in the execution context");
        }

        for (Map.Entry<String, Object> entry : new LinkedHashMap<>(params).entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String textValue) {
                if ("AS_OF_DATE".equalsIgnoreCase(textValue)) {
                    params.put(key, context.getAsOfDate());
                } else if (textValue.startsWith("AS_OF_DATE_MINUS_MONTHS:")) {
                    params.put(key, context.getAsOfDate().minusMonths(Long.parseLong(textValue.substring("AS_OF_DATE_MINUS_MONTHS:".length()))));
                } else if (textValue.startsWith("AS_OF_DATE_MINUS_DAYS:")) {
                    params.put(key, context.getAsOfDate().minusDays(Long.parseLong(textValue.substring("AS_OF_DATE_MINUS_DAYS:".length()))));
                } else if ("fromDate".equals(key) || "toDate".equals(key)) {
                    params.put(key, LocalDate.parse(textValue));
                }
            }
        }

        if (params.get("fromDate") == null && params.get("lookbackMonths") instanceof Number lookbackMonths) {
            params.put("fromDate", context.getAsOfDate().minusMonths(lookbackMonths.longValue()));
        }
        if (params.get("toDate") == null) {
            params.put("toDate", context.getAsOfDate());
        }

        validateRequiredParameters(indicatorCode, params);
        return params;
    }

    private void validateRequiredParameters(String indicatorCode, Map<String, Object> params) {
        if (!"CONSISTENT_VOLUME_SCORE".equalsIgnoreCase(indicatorCode)) {
            return;
        }
        String[] required = {
                "baselineWindow", "baselineLowPercentile", "baselineHighPercentile",
                "rvolPercentileWindow", "rvolThresholdPercentile", "consistencyWindow",
                "requiredScore", "fromDate", "toDate"
        };
        for (String key : required) {
            if (!params.containsKey(key) || params.get(key) == null) {
                throw new IllegalArgumentException("Missing required parameter for " + indicatorCode + ": " + key);
            }
        }
    }
}
