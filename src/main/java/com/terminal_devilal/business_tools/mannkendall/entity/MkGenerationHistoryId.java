package com.terminal_devilal.business_tools.mannkendall.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class MkGenerationHistoryId implements Serializable {

    private LocalDate date;
    private Integer days;

    public MkGenerationHistoryId() {
    }

    public MkGenerationHistoryId(LocalDate date, Integer days) {
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
        MkGenerationHistoryId that = (MkGenerationHistoryId) o;
        return Objects.equals(date, that.date) && Objects.equals(days, that.days);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, days);
    }
}
