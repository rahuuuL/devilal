package com.terminal_devilal.decision.indicator;

import java.util.List;
import java.util.Map;

import com.terminal_devilal.decision.entity.DecisionProfileEntity;

public interface IndicatorProvider {
	String getIndicatorCode();

	Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters, DecisionProfileEntity profile);

	default List<String> resolveTickers(IndicatorEvaluationContext context, SubjectTickerResolver resolver) {
		return resolver.resolve(context);
	}
}
