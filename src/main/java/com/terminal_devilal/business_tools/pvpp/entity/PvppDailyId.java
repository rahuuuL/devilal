package com.terminal_devilal.business_tools.pvpp.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class PvppDailyId implements Serializable {

    private String ticker;
    private LocalDate date;

    public PvppDailyId() {
    }

    public PvppDailyId(String ticker, LocalDate date) {
        this.ticker = ticker;
        this.date = date;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PvppDailyId that = (PvppDailyId) o;
        return Objects.equals(ticker, that.ticker) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, date);
    }
}
