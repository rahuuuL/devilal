package com.terminal_devilal.business_tools.mannkendall.dto;

import java.time.LocalDate;

public class MannKendallHistoryGenerateRequest {

    private LocalDate date;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
