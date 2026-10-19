package com.terminal_devilal.business_tools.ratio_analysis.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(SharpeSortinoResultHistoryId.class)
@Table(name = "sharpe_sortino_result_history")
public class SharpeSortinoResultHistoryEntity {

    @Id
    @Column(name = "ticker")
    private String ticker;

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Id
    @Column(name = "rolling_days")
    private Integer rollingDays;

    @Column(name = "sortino_ratio")
    private Double sortinoRatio;

    @Column(name = "sharpe_ratio")
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
