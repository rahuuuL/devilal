package com.terminal_devilal.indicators.common_entities;

public class TickerValue {
	private String ticker;
	private double value;

	public TickerValue(String ticker, double value) {
		this.ticker = ticker;
		this.value = value;
	}

	public String getTicker() {
		return ticker;
	}

	public double getValue() {
		return value;
	}

}
