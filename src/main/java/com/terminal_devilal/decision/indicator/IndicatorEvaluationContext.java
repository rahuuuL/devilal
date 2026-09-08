package com.terminal_devilal.decision.indicator;

public class IndicatorEvaluationContext {
    private final String subjectType;
    private final String subjectId;

    public IndicatorEvaluationContext(String subjectType, String subjectId) {
        this.subjectType = subjectType;
        this.subjectId = subjectId;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public String getSubjectId() {
        return subjectId;
    }

}
