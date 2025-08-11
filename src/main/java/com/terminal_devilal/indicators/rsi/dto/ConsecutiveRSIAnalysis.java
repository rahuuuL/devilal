package com.terminal_devilal.indicators.rsi.dto;

import java.util.List;

public class ConsecutiveRSIAnalysis {

	String ticker;
	List<Double> consecutiveRSIValues;

	public ConsecutiveRSIAnalysis(String ticker, List<Double> consecutiveRSIValues) {
		super();
		this.ticker = ticker;
		this.consecutiveRSIValues = consecutiveRSIValues;
	}

	public ConsecutiveRSIAnalysis() {
		super();
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public List<Double> getConsecutiveRSIValues() {
		return consecutiveRSIValues;
	}

	public void setConsecutiveRSIValues(List<Double> consecutiveRSIValues) {
		this.consecutiveRSIValues = consecutiveRSIValues;
	}

}
