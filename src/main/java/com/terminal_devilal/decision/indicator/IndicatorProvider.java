package com.terminal_devilal.decision.indicator;

import java.util.Map;
import java.util.List;

public interface IndicatorProvider {
    String getIndicatorCode();
    Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters);

    default List<String> resolveTickers(IndicatorEvaluationContext context, SubjectTickerResolver resolver) {
        return resolver.resolve(context);
    }
}
