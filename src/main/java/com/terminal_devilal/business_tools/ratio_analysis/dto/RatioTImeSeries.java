package com.terminal_devilal.business_tools.ratio_analysis.dto;

import java.time.LocalDate;

public class RatioTImeSeries {

	private String ticker;

	private LocalDate date;

	private double sharpeRatio;

	private double SortinoRatio;

	public RatioTImeSeries() {
		super();
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

	public double getSharpeRatio() {
		return sharpeRatio;
	}

	public void setSharpeRatio(double sharpeRatio) {
		this.sharpeRatio = sharpeRatio;
	}

	public double getSortinoRatio() {
		return SortinoRatio;
	}

	public void setSortinoRatio(double sortinoRatio) {
		SortinoRatio = sortinoRatio;
	}

}
