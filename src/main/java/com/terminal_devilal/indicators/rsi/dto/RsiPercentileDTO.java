package com.terminal_devilal.indicators.rsi.dto;

public class RsiPercentileDTO {

	public RsiPercentileDTO() {
		super();
	}

	public RsiPercentileDTO(String ticker, double rsiValue, double percentile) {
		this.ticker = ticker;
		this.rsiValue = rsiValue;
		this.percentile = percentile;
	}

	private String ticker;

	private double rsiValue;

	private double percentile;

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public double getRsiValue() {
		return rsiValue;
	}

	public void setRsiValue(double rsiValue) {
		this.rsiValue = rsiValue;
	}

	public double getPercentile() {
		return percentile;
	}

	public void setPercentile(double percentile) {
		this.percentile = percentile;
	}

}
