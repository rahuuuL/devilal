package com.terminal_devilal.business_tools.mannkendall.dto;

import java.time.LocalDate;

public class MkResultHistoryResponse {

    private String ticker;
    private LocalDate date;
    private Integer days;
    private Double score;
    private String trend;
    private Boolean h;
    private Double p;
    private Double z;
    private Double tau;
    private Double s;
    private Double var_s;
    private Double slope;
    private Double intercept;

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public Boolean getH() {
        return h;
    }

    public void setH(Boolean h) {
        this.h = h;
    }

    public Double getP() {
        return p;
    }

    public void setP(Double p) {
        this.p = p;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public Double getTau() {
        return tau;
    }

    public void setTau(Double tau) {
        this.tau = tau;
    }

    public Double getS() {
        return s;
    }

    public void setS(Double s) {
        this.s = s;
    }

    public Double getVar_s() {
        return var_s;
    }

    public void setVar_s(Double var_s) {
        this.var_s = var_s;
    }

    public Double getSlope() {
        return slope;
    }

    public void setSlope(Double slope) {
        this.slope = slope;
    }

    public Double getIntercept() {
        return intercept;
    }

    public void setIntercept(Double intercept) {
        this.intercept = intercept;
    }
}