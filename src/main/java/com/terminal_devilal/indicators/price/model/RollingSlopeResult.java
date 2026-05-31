package com.terminal_devilal.indicators.price.model;

import java.time.LocalDate;

public class RollingSlopeResult {

	private String ticker;
	private LocalDate windowEndDate;
	private double slope;
	private double percentile;

	public RollingSlopeResult() {
	}

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

	public double getPercentile() {
		return percentile;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public void setWindowEndDate(LocalDate windowEndDate) {
		this.windowEndDate = windowEndDate;
	}

	public void setSlope(double slope) {
		this.slope = slope;
	}

	public void setPercentile(double percentile) {
		this.percentile = percentile;
	}
}