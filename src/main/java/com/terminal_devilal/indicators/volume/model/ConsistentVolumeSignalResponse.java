package com.terminal_devilal.indicators.volume.model;

import java.time.LocalDate;

import com.terminal_devilal.common.model.TickerDetails;

public class ConsistentVolumeSignalResponse extends TickerDetails {

	private String ticker;
	private LocalDate date; // usually == toDate

	private double rvol; // how large is volume vs normal
	private int consistencyScore; // how many strong bars
	private int consistencyWindow; // context (e.g. 10)

	private boolean signal; // true if score >= requiredScore

	private double volume;
	private int requiredScore;

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

	public double getRvol() {
		return rvol;
	}

	public void setRvol(double rvol) {
		this.rvol = rvol;
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

	public boolean isSignal() {
		return signal;
	}

	public void setSignal(boolean signal) {
		this.signal = signal;
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	public int getRequiredScore() {
		return requiredScore;
	}

	public void setRequiredScore(int requiredScore) {
		this.requiredScore = requiredScore;
	}

	public ConsistentVolumeSignalResponse(String isin, String macro, String sector, String industry,
			String basicIndustry, String ticker, LocalDate date, double totalTradedVolume, double totalTradedValue,
			double totalMarketCap, double ffmc, double impactCost, double cmDailyVolatility, double cmAnnualVolatility,
			String ticker2, LocalDate date2, double rvol, int consistencyScore, int consistencyWindow, boolean signal,
			double volume, int requiredScore) {
		super(isin, macro, sector, industry, basicIndustry, ticker, date, totalTradedVolume, totalTradedValue,
				totalMarketCap, ffmc, impactCost, cmDailyVolatility, cmAnnualVolatility);
		ticker = ticker2;
		date = date2;
		this.rvol = rvol;
		this.consistencyScore = consistencyScore;
		this.consistencyWindow = consistencyWindow;
		this.signal = signal;
		this.volume = volume;
		this.requiredScore = requiredScore;
	}

	public ConsistentVolumeSignalResponse() {
		super();
	}

}