package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;

public class MannKendallIndicatorEvaluationContext extends IndicatorEvaluationContext {
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer days;

    public MannKendallIndicatorEvaluationContext(String subjectType, String subjectId) {
        super(subjectType, subjectId);
    }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate value) { fromDate = value; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate value) { toDate = value; }
    public Integer getDays() { return days; }
    public void setDays(Integer value) { days = value; }
}