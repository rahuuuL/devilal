package com.terminal_devilal.business_tools.ratio_analysis.dto;

import java.time.LocalDate;

public class RatioTImeSeries {

	private String ticker;

	private LocalDate date;

	private double high;

	private double low;

	private double open;

	private double close;

	private double lastTradeValue;

	private double prevoiusClosePrice;

	private long volume;

	private double value;

	private int trades;

	private long deliveryTrade;

	private double deliveryPercentage;

	private double vwap;

	private double sharpeRatio;

	private double SortinoRatio;

	public RatioTImeSeries() {
		super();
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

	public int getTrades() {
		return trades;
	}

	public void setTrades(int trades) {
		this.trades = trades;
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

	public double getSharpeRatio() {
		return sharpeRatio;
	}

	public void setSharpeRatio(double sharpeRatio) {
		this.sharpeRatio = sharpeRatio;
	}

	public double getSortinoRatio() {
		return SortinoRatio;
	}

	public void setSortinoRatio(double sortinoRatio) {
		SortinoRatio = sortinoRatio;
	}

}
