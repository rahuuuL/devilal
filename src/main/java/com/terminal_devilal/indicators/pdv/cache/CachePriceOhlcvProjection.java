package com.terminal_devilal.indicators.pdv.cache;

import java.time.LocalDate;

import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.entity.projections.PriceOhlcvProjection;

public class CachePriceOhlcvProjection implements PriceOhlcvProjection {

    private final String ticker;
    private final LocalDate date;
    private final double open;
    private final double close;
    private final double high;
    private final double low;
    private final double deliveryPercentage;
    private final long volume;
    private final double vwap;

    public CachePriceOhlcvProjection(PriceDeliveryVolumeEntity entity) {
        this.ticker = entity.getTicker();
        this.date = entity.getDate();
        this.open = entity.getOpen();
        this.close = entity.getClose();
        this.high = entity.getHigh();
        this.low = entity.getLow();
        this.deliveryPercentage = entity.getDeliveryPercentage();
        this.volume = entity.getVolume();
        this.vwap = entity.getVwap();
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
    public double getOpen() {
        return open;
    }

    @Override
    public double getClose() {
        return close;
    }

    @Override
    public double getHigh() {
        return high;
    }

    @Override
    public double getLow() {
        return low;
    }

    @Override
    public double getDeliveryPercentage() {
        return deliveryPercentage;
    }

    @Override
    public long getVolume() {
        return volume;
    }

    @Override
    public double getVwap() {
        return vwap;
    }
}
