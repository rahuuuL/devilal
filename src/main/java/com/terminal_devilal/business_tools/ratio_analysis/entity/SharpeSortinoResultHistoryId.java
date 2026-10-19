package com.terminal_devilal.business_tools.ratio_analysis.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class SharpeSortinoResultHistoryId implements Serializable {

    private static final long serialVersionUID = 1L;
    private String ticker;
    private LocalDate date;
    private Integer rollingDays;

    public SharpeSortinoResultHistoryId() {
    }

    public SharpeSortinoResultHistoryId(String ticker, LocalDate date, Integer rollingDays) {
        this.ticker = ticker;
        this.date = date;
        this.rollingDays = rollingDays;
    }

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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        SharpeSortinoResultHistoryId that = (SharpeSortinoResultHistoryId) other;
        return Objects.equals(ticker, that.ticker)
                && Objects.equals(date, that.date)
                && Objects.equals(rollingDays, that.rollingDays);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, date, rollingDays);
    }
}
