package com.terminal_devilal.business_tools.pvpp.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class PvppGenerationHistoryId implements Serializable {

    private LocalDate date;
    private Integer days;

    public PvppGenerationHistoryId() {
    }

    public PvppGenerationHistoryId(LocalDate date, Integer days) {
        this.date = date;
        this.days = days;
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
        PvppGenerationHistoryId that = (PvppGenerationHistoryId) o;
        return Objects.equals(date, that.date) && Objects.equals(days, that.days);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, days);
    }
}
