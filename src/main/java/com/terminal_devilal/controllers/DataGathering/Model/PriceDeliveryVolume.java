package com.terminal_devilal.controllers.DataGathering.Model;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Entity
@Table(name = "pdv")
@IdClass(PriceDeliveryVolumeId.class)
public class PriceDeliveryVolume {

	@Id
    @Column(name = "ticker")
    private String ticker;

	@Id
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "high")
    private double high;

    @Column(name = "low")
    private double low;

    @Column(name = "open")
    private double open;

    @Column(name = "close")
    private double close;

    @Column(name = "ltp")
    private double lastTradeValue;

    @Column(name = "prev_close")
    private double prevoiusClosePrice;

    @Column(name = "volume")
    private long volume;

    @Column(name = "value")
    private double value;

    @Column(name = "52w_high")
    private double week52High;

    @Column(name = "52w_low")
    private double week52Low;

    @Column(name = "trades")
    private int trades;

    @Column(name = "isin")
    private String isin;

    @Column(name = "del_trade")
    private long deliveryTrade;

    @Column(name = "del_percent")
    private double deliveryPercentage;

    @Column(name = "vwap")
    private double vwap;
    
    // Mapper Utiliy function
    public static PriceDeliveryVolume parseStockData(JsonNode item) {
    	
    	PriceDeliveryVolume stock = new PriceDeliveryVolume();
    	
        stock.setTicker(item.path("CH_SYMBOL").asText());
        stock.setDate(LocalDate.parse(item.get("CH_TIMESTAMP").asText()));
        stock.setHigh(item.path("CH_TRADE_HIGH_PRICE").asDouble());
        stock.setLow(item.path("CH_TRADE_LOW_PRICE").asDouble());
        stock.setOpen(item.path("CH_OPENING_PRICE").asDouble());
        stock.setClose(item.path("CH_CLOSING_PRICE").asDouble());
        stock.setLastTradeValue(item.path("CH_LAST_TRADED_PRICE").asDouble());
        stock.setPrevoiusClosePrice(item.path("CH_PREVIOUS_CLS_PRICE").asDouble());
        stock.setVolume(item.path("CH_TOT_TRADED_QTY").asLong());
        stock.setValue(item.path("CH_TOT_TRADED_VAL").asDouble());
        stock.setWeek52High(item.path("CH_52WEEK_HIGH_PRICE").asDouble());
        stock.setWeek52Low(item.path("CH_52WEEK_LOW_PRICE").asDouble());
        stock.setTrades(item.path("CH_TOTAL_TRADES").asInt());
        stock.setIsin(item.path("CH_ISIN").asText());
        stock.setDeliveryTrade(item.path("COP_DELIV_QTY").asLong());
        stock.setDeliveryPercentage(item.path("COP_DELIV_PERC").asDouble());
        stock.setVwap(item.path("VWAP").asDouble());

        return stock;
    }
    
    
    // Getters and Setters
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

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getLastTradeValue() {
        return lastTradeValue;
    }

    public void setLastTradeValue(double lastTradeValue) {
        this.lastTradeValue = lastTradeValue;
    }

    public double getPrevoiusClosePrice() {
        return prevoiusClosePrice;
    }

    public void setPrevoiusClosePrice(double prevoiusClosePrice) {
        this.prevoiusClosePrice = prevoiusClosePrice;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getWeek52High() {
        return week52High;
    }

    public void setWeek52High(double week52High) {
        this.week52High = week52High;
    }

    public double getWeek52Low() {
        return week52Low;
    }

    public void setWeek52Low(double week52Low) {
        this.week52Low = week52Low;
    }

    public int getTrades() {
        return trades;
    }

    public void setTrades(int trades) {
        this.trades = trades;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public long getDeliveryTrade() {
        return deliveryTrade;
    }

    public void setDeliveryTrade(long deliveryTrade) {
        this.deliveryTrade = deliveryTrade;
    }

    public double getDeliveryPercentage() {
        return deliveryPercentage;
    }

    public void setDeliveryPercentage(double deliveryPercentage) {
        this.deliveryPercentage = deliveryPercentage;
    }

    public double getVwap() {
        return vwap;
    }

    public void setVwap(double vwap) {
        this.vwap = vwap;
    }
}

