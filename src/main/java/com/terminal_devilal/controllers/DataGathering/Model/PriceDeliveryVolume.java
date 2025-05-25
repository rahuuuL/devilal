package com.terminal_devilal.controllers.DataGathering.Model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Entity
@Table(name = "pdvt")
@IdClass(TickerDateId.class)
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

	// Static date formatter reused to avoid re-creating it every time
	private static final DateTimeFormatter NSE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy",
			Locale.ENGLISH);

	// Mapper Utility function
	public static TreeSet<PriceDeliveryVolume> parseStockData(JsonNode node, String ticker) {
		TreeSet<PriceDeliveryVolume> stockList = new TreeSet<PriceDeliveryVolume>(Comparator.comparing(PriceDeliveryVolume::getDate));
		JsonNode dataArray = node.path("data");

		if (!dataArray.isArray()) {
			System.out.print("Empty date" + node.toPrettyString());
			return stockList; // return empty if "data" is not an array
		}

		for (JsonNode item : dataArray) {
			PriceDeliveryVolume stock = new PriceDeliveryVolume();

			stock.setTicker(ticker);
			stock.setDate(parseDate(item.path("mTIMESTAMP").asText("")));
			stock.setHigh(item.path("CH_TRADE_HIGH_PRICE").asDouble(0.0));
			stock.setLow(item.path("CH_TRADE_LOW_PRICE").asDouble(0.0));
			stock.setOpen(item.path("CH_OPENING_PRICE").asDouble(0.0));
			stock.setClose(item.path("CH_CLOSING_PRICE").asDouble(0.0));
			stock.setLastTradeValue(item.path("CH_LAST_TRADED_PRICE").asDouble(0.0));
			stock.setPrevoiusClosePrice(item.path("CH_PREVIOUS_CLS_PRICE").asDouble(0.0));
			stock.setVolume(item.path("CH_TOT_TRADED_QTY").asLong(0L));
			stock.setValue(item.path("CH_TOT_TRADED_VAL").asDouble(0.0));
			stock.setTrades(item.path("CH_TOTAL_TRADES").asInt(0));
			stock.setDeliveryTrade(item.path("COP_DELIV_QTY").asLong(0L));
			stock.setDeliveryPercentage(item.path("COP_DELIV_PERC").asDouble(0.0));
			stock.setVwap(item.path("VWAP").asDouble(0.0));

			stockList.add(stock);
		}

		return stockList;
	}

	// Helper function to parse dates safely
	private static LocalDate parseDate(String dateStr) {
		try {
			return LocalDate.parse(dateStr, NSE_DATE_FORMATTER);
		} catch (Exception e) {
			return null; // Or LocalDate.now() or throw, based on your requirement
		}
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
