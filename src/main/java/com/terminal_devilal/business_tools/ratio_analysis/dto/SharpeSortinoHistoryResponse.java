package com.terminal_devilal.business_tools.ratio_analysis.dto;

import java.time.LocalDate;

public class SharpeSortinoHistoryResponse {

    private String ticker;
    private LocalDate date;
    private Integer rollingDays;
    private Double sortinoRatio;
    private Double sharpeRatio;
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

    public Integer getRollingDays() {
        return rollingDays;
    }

    public void setRollingDays(Integer rollingDays) {
        this.rollingDays = rollingDays;
    }

    public Double getSortinoRatio() {
        return sortinoRatio;
    }

    public void setSortinoRatio(Double sortinoRatio) {
        this.sortinoRatio = sortinoRatio;
    }

    public Double getSharpeRatio() {
        return sharpeRatio;
    }

    public void setSharpeRatio(Double sharpeRatio) {
        this.sharpeRatio = sharpeRatio;
    }

}
