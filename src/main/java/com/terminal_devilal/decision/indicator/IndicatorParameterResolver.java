package com.terminal_devilal.decision.indicator;

import com.terminal_devilal.decision.model.RuleDefinition;
import java.util.Map;

public interface IndicatorParameterResolver {
    Map<String, Object> resolve(String indicatorCode, RuleDefinition.Condition condition, IndicatorEvaluationContext context);
}
