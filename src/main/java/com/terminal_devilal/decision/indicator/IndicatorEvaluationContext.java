package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;

public class IndicatorEvaluationContext {
    private final String subjectType;
    private final String subjectId;
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

    public Integer getBaselineWindow() {
        return baselineWindow;
    }

    public void setBaselineWindow(Integer baselineWindow) {
        this.baselineWindow = baselineWindow;
    }

    public Double getBaselineLowPercentile() {
        return baselineLowPercentile;
    }

    public void setBaselineLowPercentile(Double baselineLowPercentile) {
        this.baselineLowPercentile = baselineLowPercentile;
    }

    public Double getBaselineHighPercentile() {
        return baselineHighPercentile;
    }

    public void setBaselineHighPercentile(Double baselineHighPercentile) {
        this.baselineHighPercentile = baselineHighPercentile;
    }

    public Integer getRvolPercentileWindow() {
        return rvolPercentileWindow;
    }

    public void setRvolPercentileWindow(Integer rvolPercentileWindow) {
        this.rvolPercentileWindow = rvolPercentileWindow;
    }

    public Double getRvolThresholdPercentile() {
        return rvolThresholdPercentile;
    }

    public void setRvolThresholdPercentile(Double rvolThresholdPercentile) {
        this.rvolThresholdPercentile = rvolThresholdPercentile;
    }

    public Integer getConsistencyWindow() {
        return consistencyWindow;
    }

    public void setConsistencyWindow(Integer consistencyWindow) {
        this.consistencyWindow = consistencyWindow;
    }

    public Integer getRequiredScore() {
        return requiredScore;
    }

    public void setRequiredScore(Integer requiredScore) {
        this.requiredScore = requiredScore;
    }

    public Integer getLookbackMonths() {
        return lookbackMonths;
    }

    public void setLookbackMonths(Integer lookbackMonths) {
        this.lookbackMonths = lookbackMonths;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }
}
