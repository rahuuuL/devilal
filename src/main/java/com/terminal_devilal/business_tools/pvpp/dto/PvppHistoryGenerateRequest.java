package com.terminal_devilal.business_tools.pvpp.dto;

import java.time.LocalDate;

public class PvppHistoryGenerateRequest {

    private LocalDate date;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
