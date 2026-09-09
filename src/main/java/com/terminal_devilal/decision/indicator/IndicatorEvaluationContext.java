package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;

public class IndicatorEvaluationContext {
    private final String subjectType;
    private final String subjectId;
    private final LocalDate asOfDate;

    public IndicatorEvaluationContext(String subjectType, String subjectId, LocalDate asOfDate) {
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.asOfDate = asOfDate;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }
}
