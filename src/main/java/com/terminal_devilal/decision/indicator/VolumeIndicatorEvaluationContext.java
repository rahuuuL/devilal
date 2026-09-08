package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;

public class VolumeIndicatorEvaluationContext extends IndicatorEvaluationContext {
    private final LocalDate asOfDate;
    private Integer baselineWindow;
    private Double baselineLowPercentile;
    private Double baselineHighPercentile;
    private Integer rvolPercentileWindow;
    private Double rvolThresholdPercentile;
    private Integer consistencyWindow;
    private Integer requiredScore;
    private Integer lookbackMonths;
    private LocalDate fromDate;
    private LocalDate toDate;

    public VolumeIndicatorEvaluationContext(String subjectType, String subjectId, LocalDate asOfDate) {
        super(subjectType, subjectId);
        this.asOfDate = asOfDate;
    }

    public LocalDate getAsOfDate() { return asOfDate; }
    public Integer getBaselineWindow() { return baselineWindow; }
    public void setBaselineWindow(Integer value) { baselineWindow = value; }
    public Double getBaselineLowPercentile() { return baselineLowPercentile; }
    public void setBaselineLowPercentile(Double value) { baselineLowPercentile = value; }
    public Double getBaselineHighPercentile() { return baselineHighPercentile; }
    public void setBaselineHighPercentile(Double value) { baselineHighPercentile = value; }
    public Integer getRvolPercentileWindow() { return rvolPercentileWindow; }
    public void setRvolPercentileWindow(Integer value) { rvolPercentileWindow = value; }
    public Double getRvolThresholdPercentile() { return rvolThresholdPercentile; }
    public void setRvolThresholdPercentile(Double value) { rvolThresholdPercentile = value; }
    public Integer getConsistencyWindow() { return consistencyWindow; }
    public void setConsistencyWindow(Integer value) { consistencyWindow = value; }
    public Integer getRequiredScore() { return requiredScore; }
    public void setRequiredScore(Integer value) { requiredScore = value; }
    public Integer getLookbackMonths() { return lookbackMonths; }
    public void setLookbackMonths(Integer value) { lookbackMonths = value; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate value) { fromDate = value; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate value) { toDate = value; }
}