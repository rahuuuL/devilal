package com.terminal_devilal.controllers.Functional.MannKendall.Model;

import java.time.LocalDate;

import com.terminal_devilal.controllers.DataGathering.Model.TradeInfo;

public class MannKendallAPIResponse extends MannKendallResponse {

	private String ticker;

	private LocalDate tradeInfoDate;

	private double totalTradedVolume;

	private double totalTradedValue;

	private double totalMarketCap;

	private double ffmc;

	private double impactCost;

	private double cmDailyVolatility;

	private double cmAnnualVolatility;

	public MannKendallAPIResponse() {
		super();
	}

	public MannKendallAPIResponse(String ticker, TradeInfo info, MannKendallResponse response) {
		super();
		this.ticker = ticker;

		// Copy all trade info details from info to this object
		setTradeInfo(info);

		// Copy all fields from response to this
		this.setTrend(response.getTrend());
		this.setH(response.getH());
		this.setP(response.getP());
		this.setZ(response.getZ());
		this.setTau(response.getTau());
		this.setS(response.getS());
		this.setVar_s(response.getVar_s());
		this.setSlope(response.getSlope());
		this.setIntercept(response.getIntercept());
	}

	public void setTradeInfo(TradeInfo info) {
		// Copy all trade info details from info to this object
		this.tradeInfoDate = info.getDate();
		this.totalTradedVolume = info.getTotalTradedVolume();
		this.totalTradedValue = info.getTotalTradedValue();
		this.totalMarketCap = info.getTotalMarketCap();
		this.ffmc = info.getFfmc();
		this.impactCost = info.getImpactCost();
		this.cmDailyVolatility = info.getCmDailyVolatility();
		this.cmAnnualVolatility = info.getCmAnnualVolatility();
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

	@Override
	public String toString() {
		return "MannKendallAPIResponse [ticker=" + ticker + ", tradeInfoDate=" + tradeInfoDate + ", totalTradedVolume="
				+ totalTradedVolume + ", totalTradedValue=" + totalTradedValue + ", totalMarketCap=" + totalMarketCap
				+ ", ffmc=" + ffmc + ", impactCost=" + impactCost + ", cmDailyVolatility=" + cmDailyVolatility
				+ ", cmAnnualVolatility=" + cmAnnualVolatility + "]";
	}

}
