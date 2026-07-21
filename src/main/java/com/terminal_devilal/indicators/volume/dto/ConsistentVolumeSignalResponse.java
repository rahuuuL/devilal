package com.terminal_devilal.indicators.volume.model;

import java.time.LocalDate;

public class ConsistentVolumeSignalResponse {

	private String ticker;
	private LocalDate date; // usually == toDate
	private int consistencyScore; // how many strong bars
	private int consistencyWindow; // context (e.g. 10)
	private double relativeVolumesCombinedAverage;

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

	public int getConsistencyScore() {
		return consistencyScore;
	}

	public void setConsistencyScore(int consistencyScore) {
		this.consistencyScore = consistencyScore;
	}

	public int getConsistencyWindow() {
		return consistencyWindow;
	}

	public void setConsistencyWindow(int consistencyWindow) {
		this.consistencyWindow = consistencyWindow;
	}

	public double getRelativeVolumesCombinedAverage() {
		return relativeVolumesCombinedAverage;
	}

	public void setRelativeVolumesCombinedAverage(double relativeVolumesCombinedAverage) {
		this.relativeVolumesCombinedAverage = relativeVolumesCombinedAverage;
	}

	public ConsistentVolumeSignalResponse() {
		super();
	}

}