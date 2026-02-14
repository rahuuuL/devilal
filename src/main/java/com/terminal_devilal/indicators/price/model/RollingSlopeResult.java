package com.terminal_devilal.indicators.price.model;

import java.time.LocalDate;

public class RollingSlopeResult {
	private final String ticker;
	private final LocalDate windowEndDate;
	private final double slope;

	public RollingSlopeResult(String ticker, LocalDate windowEndDate, double slope) {
		this.ticker = ticker;
		this.windowEndDate = windowEndDate;
		this.slope = slope;
	}

	public String getTicker() {
		return ticker;
	}

	public LocalDate getWindowEndDate() {
		return windowEndDate;
	}

	public double getSlope() {
		return slope;
	}
}