package com.terminal_devilal.indicators.pdv.cache;

import java.time.LocalDate;

import com.terminal_devilal.indicators.pdv.entity.projections.ConsistentVolumeProjection;

public class CacheConsistentVolumeProjection implements ConsistentVolumeProjection {

    private final String ticker;
    private final LocalDate date;
    private final Long volume;

    public CacheConsistentVolumeProjection(String ticker, LocalDate date, Long volume) {
        this.ticker = ticker;
        this.date = date;
        this.volume = volume;
    }

    @Override
    public String getTicker() {
        return ticker;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public Long getVolume() {
        return volume;
    }
}
