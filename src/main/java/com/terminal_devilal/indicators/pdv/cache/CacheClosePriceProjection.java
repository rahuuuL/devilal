package com.terminal_devilal.indicators.pdv.cache;

import java.time.LocalDate;

import com.terminal_devilal.indicators.pdv.entity.projections.ClosePriceProjection;

public class CacheClosePriceProjection implements ClosePriceProjection {

    private final String ticker;
    private final LocalDate date;
    private final double close;

    public CacheClosePriceProjection(String ticker, LocalDate date, double close) {
        this.ticker = ticker;
        this.date = date;
        this.close = close;
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
    public double getClose() {
        return close;
    }
}
