package com.terminal_devilal.indicators.pdv.dto;

import java.time.LocalDate;

public class PDVCacheReloadRequest {

    private LocalDate date;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}