package com.terminal_devilal.Model.Meta;

import java.time.LocalDate;

public class TradeInfoMeta {

	private String ticker;

	private LocalDate tradeInfoDate;

	private double totalTradedVolume;

	private double totalTradedValue;

	private double totalMarketCap;

	private double ffmc;

	private double impactCost;

	private double cmDailyVolatility;

	private double cmAnnualVolatility;

	public TradeInfoMeta(String ticker, LocalDate tradeInfoDate, double totalTradedVolume, double totalTradedValue,
			double totalMarketCap, double ffmc, double impactCost, double cmDailyVolatility,
			double cmAnnualVolatility) {
		super();
		this.ticker = ticker;
		this.tradeInfoDate = tradeInfoDate;
		this.totalTradedVolume = totalTradedVolume;
		this.totalTradedValue = totalTradedValue;
		this.totalMarketCap = totalMarketCap;
		this.ffmc = ffmc;
		this.impactCost = impactCost;
		this.cmDailyVolatility = cmDailyVolatility;
		this.cmAnnualVolatility = cmAnnualVolatility;
	}

	public TradeInfoMeta() {
		this.ticker = "";
		this.tradeInfoDate = LocalDate.MIN;
		this.totalTradedVolume = 0.0;
		this.totalTradedValue = 0.0;
		this.totalMarketCap = 0.0;
		this.ffmc = 0.0;
		this.impactCost = 0.0;
		this.cmDailyVolatility = 0.0;
		this.cmAnnualVolatility = 0.0;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
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

}
