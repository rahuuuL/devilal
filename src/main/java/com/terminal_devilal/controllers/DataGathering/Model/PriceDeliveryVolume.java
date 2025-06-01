package com.terminal_devilal.controllers.DataGathering.Model;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@IdClass(TickerDateId.class)
@Table(name = "pdvt", indexes = { @Index(name = "idx_date", columnList = "date") }) // this creates an index on date
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

	@Column(name = "trades")
	private int trades;

	@Column(name = "del_trade")
	private long deliveryTrade;

	@Column(name = "del_percent")
	private double deliveryPercentage;

	@Column(name = "vwap")
	private double vwap;

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

	@Override
	public int hashCode() {
		return Objects.hash(date, ticker);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PriceDeliveryVolume other = (PriceDeliveryVolume) obj;
		return Objects.equals(date, other.date) && Objects.equals(ticker, other.ticker);
	}

	@Override
	public String toString() {
		return "PriceDeliveryVolume [ticker=" + ticker + ", date=" + date + ", high=" + high + ", low=" + low
				+ ", open=" + open + ", close=" + close + ", lastTradeValue=" + lastTradeValue + ", prevoiusClosePrice="
				+ prevoiusClosePrice + ", volume=" + volume + ", value=" + value + ", trades=" + trades
				+ ", deliveryTrade=" + deliveryTrade + ", deliveryPercentage=" + deliveryPercentage + ", vwap=" + vwap
				+ "]";
	}

}
