package com.terminal_devilal.decision.indicator;

import java.util.Map;

public interface IndicatorProvider {
    String getIndicatorCode();
    Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters);
}
