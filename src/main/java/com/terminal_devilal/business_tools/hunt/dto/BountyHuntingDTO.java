package com.terminal_devilal.business_tools.hunt.dto;

import java.time.LocalDate;

public class BountyHuntingDTO {

	private String ticker;

	private double percentageChange;

	private LocalDate tradeInfoDate;

	private double totalTradedVolume;

	private double totalTradedValue;

	private double totalMarketCap;

	private double ffmc;

	private double impactCost;

	private double cmDailyVolatility;

	private double cmAnnualVolatility;

	private double rawSharpe;

	private double rawSortino;

	private int ShapreDays;

	public int getShapreDays() {
		return ShapreDays;
	}

	public void setShapreDays(int shapreDays) {
		ShapreDays = shapreDays;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public double getPercentageChange() {
		return percentageChange;
	}

	public void setPercentageChange(double percentageChange) {
		this.percentageChange = percentageChange;
	}

	public LocalDate getTradeInfoDate() {
		return tradeInfoDate;
	}

	public void setTradeInfoDate(LocalDate tradeInfoDate) {
		this.tradeInfoDate = tradeInfoDate;
	}

	public double getTotalTradedVolume() {
		return totalTradedVolume;
	}

	public void setTotalTradedVolume(double totalTradedVolume) {
		this.totalTradedVolume = totalTradedVolume;
	}

	public double getTotalTradedValue() {
		return totalTradedValue;
	}

	public void setTotalTradedValue(double totalTradedValue) {
		this.totalTradedValue = totalTradedValue;
	}

	public double getTotalMarketCap() {
		return totalMarketCap;
	}

	public void setTotalMarketCap(double totalMarketCap) {
		this.totalMarketCap = totalMarketCap;
	}

	public double getFfmc() {
		return ffmc;
	}

	public void setFfmc(double ffmc) {
		this.ffmc = ffmc;
	}

	public double getImpactCost() {
		return impactCost;
	}

	public void setImpactCost(double impactCost) {
		this.impactCost = impactCost;
	}

	public double getCmDailyVolatility() {
		return cmDailyVolatility;
	}

	public void setCmDailyVolatility(double cmDailyVolatility) {
		this.cmDailyVolatility = cmDailyVolatility;
	}

	public double getCmAnnualVolatility() {
		return cmAnnualVolatility;
	}

	public void setCmAnnualVolatility(double cmAnnualVolatility) {
		this.cmAnnualVolatility = cmAnnualVolatility;
	}

	public double getRawSharpe() {
		return rawSharpe;
	}

	public void setRawSharpe(double rawSharpe) {
		this.rawSharpe = rawSharpe;
	}

	public double getRawSortino() {
		return rawSortino;
	}

	public void setRawSortino(double rawSortino) {
		this.rawSortino = rawSortino;
	}

	public BountyHuntingDTO(String ticker, double percentageChange, LocalDate tradeInfoDate, double totalTradedVolume,
			double totalTradedValue, double totalMarketCap, double ffmc, double impactCost, double cmDailyVolatility,
			double cmAnnualVolatility, double rawSharpe, double rawSortino, int shapreDays) {
		super();
		this.ticker = ticker;
		this.percentageChange = percentageChange;
		this.tradeInfoDate = tradeInfoDate;
		this.totalTradedVolume = totalTradedVolume;
		this.totalTradedValue = totalTradedValue;
		this.totalMarketCap = totalMarketCap;
		this.ffmc = ffmc;
		this.impactCost = impactCost;
		this.cmDailyVolatility = cmDailyVolatility;
		this.cmAnnualVolatility = cmAnnualVolatility;
		this.rawSharpe = rawSharpe;
		this.rawSortino = rawSortino;
		ShapreDays = shapreDays;
	}

}
