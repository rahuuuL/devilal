package com.terminal_devilal.controllers.Functional.SharpeRatio.Model;

public class SharpeRatioDTO {
	private double rawSharpe;
	private double annualizedSharpe;
	private int daysUsed;

	public SharpeRatioDTO(double rawSharpe, double annualizedSharpe, int daysUsed) {
		this.rawSharpe = rawSharpe;
		this.annualizedSharpe = annualizedSharpe;
		this.daysUsed = daysUsed;
	}

	public double getRawSharpe() {
		return rawSharpe;
	}

	public void setRawSharpe(double rawSharpe) {
		this.rawSharpe = rawSharpe;
	}

	public double getAnnualizedSharpe() {
		return annualizedSharpe;
	}

	public void setAnnualizedSharpe(double annualizedSharpe) {
		this.annualizedSharpe = annualizedSharpe;
	}

	public int getDaysUsed() {
		return daysUsed;
	}

	public void setDaysUsed(int daysUsed) {
		this.daysUsed = daysUsed;
	}

	@Override
	public String toString() {
		return "SharpeRatioDTO [rawSharpe=" + rawSharpe + ", annualizedSharpe=" + annualizedSharpe + ", daysUsed="
				+ daysUsed + "]";
	}

}
