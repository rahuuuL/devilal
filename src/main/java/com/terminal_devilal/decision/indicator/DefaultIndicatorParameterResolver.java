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

        if ("CONSISTENT_VOLUME_SCORE".equalsIgnoreCase(indicatorCode)) {
            // This can be made dynamic in the future if needed, but for now, we will use fixed values for the default parameters.
            LocalDate asOfDate = context.getAsOfDate();
            LocalDate fromDate = asOfDate.minusMonths(18);
            LocalDate toDate = asOfDate;

            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
            params.put("baselineWindow", 20);
            params.put("baselineLowPercentile", 20.0);
            params.put("baselineHighPercentile", 80.0);
            params.put("rvolPercentileWindow", 60);
            params.put("rvolThresholdPercentile", 75.0);
            params.put("consistencyWindow", 10);
            params.put("requiredScore", 7);
        }

        Map<String, Object> customParameters = condition == null || condition.parameters() == null
                ? Map.of()
                : condition.parameters();
        params.putAll(customParameters);

        if (customParameters.containsKey("fromDate") && customParameters.get("fromDate") instanceof String fromDate) {
            params.put("fromDate", LocalDate.parse(fromDate));
        }
        if (customParameters.containsKey("toDate") && customParameters.get("toDate") instanceof String toDate) {
            params.put("toDate", LocalDate.parse(toDate));
        }

        if (customParameters.containsKey("lookbackMonths")
                && !customParameters.containsKey("fromDate")
                && params.get("lookbackMonths") instanceof Number lookbackMonths) {
            params.put("fromDate", context.getAsOfDate().minusMonths(lookbackMonths.longValue()));
        } else if (params.get("fromDate") == null && params.get("lookbackMonths") instanceof Number lookbackMonths) {
            params.put("fromDate", context.getAsOfDate().minusMonths(lookbackMonths.longValue()));
        }
        if (params.get("toDate") == null) {
            params.put("toDate", context.getAsOfDate());
        }

        return params;
    }
}
