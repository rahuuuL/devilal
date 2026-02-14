package com.terminal_devilal.indicators.common_entities;

import java.time.LocalDate;

public class TickerValue {
	private String ticker;
	private LocalDate date;
	private double value;

	public TickerValue(String ticker, LocalDate date, double value) {
		super();
		this.ticker = ticker;
		this.date = date;
		this.value = value;
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

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

}
