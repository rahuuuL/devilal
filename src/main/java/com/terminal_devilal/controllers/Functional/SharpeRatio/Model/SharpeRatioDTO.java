package com.terminal_devilal.controllers.Functional.SharpeRatio.Model;

public class SharpeRatioDTO {
	private double rawSharpe;
	private double annualizedSharpe;
	private int daysUsed;
	private double rawSortino;

	public SharpeRatioDTO(double rawSharpe, double annualizedSharpe, int daysUsed, double rawSortino) {
		super();
		this.rawSharpe = rawSharpe;
		this.annualizedSharpe = annualizedSharpe;
		this.daysUsed = daysUsed;
		this.rawSortino = rawSortino;
	}

	public double getRawSortino() {
		return rawSortino;
	}

	public void setRawSortino(double rawSortino) {
		this.rawSortino = rawSortino;
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
