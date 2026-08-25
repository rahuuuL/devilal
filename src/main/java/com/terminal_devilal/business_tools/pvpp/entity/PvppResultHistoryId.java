package com.terminal_devilal.business_tools.pvpp.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class PvppResultHistoryId implements Serializable {

    private String ticker;
    private LocalDate date;
    private Integer days;

    public PvppResultHistoryId() {
    }

    public PvppResultHistoryId(String ticker, LocalDate date, Integer days) {
        this.ticker = ticker;
        this.date = date;
        this.days = days;
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

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PvppResultHistoryId that = (PvppResultHistoryId) o;
        return Objects.equals(ticker, that.ticker)
                && Objects.equals(date, that.date)
                && Objects.equals(days, that.days);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, date, days);
    }
}
