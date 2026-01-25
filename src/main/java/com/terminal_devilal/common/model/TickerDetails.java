package com.terminal_devilal.common.model;

import java.time.LocalDate;

public class TickerDetails {

	private String isin;

	private String macro;

	private String sector;

	private String industry;

	private String basicIndustry;

	private String ticker;

	private LocalDate detailsDate;

	private double totalTradedVolume;

	private double totalTradedValue;

	private double totalMarketCap;

	private double ffmc;

	private double impactCost;

	private double cmDailyVolatility;

	private double cmAnnualVolatility;

	public String getIsin() {
		return isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public String getMacro() {
		return macro;
	}

	public void setMacro(String macro) {
		this.macro = macro;
	}

	public String getSector() {
		return sector;
	}

	public void setSector(String sector) {
		this.sector = sector;
	}

	public String getIndustry() {
		return industry;
	}

	public void setIndustry(String industry) {
		this.industry = industry;
	}

	public String getBasicIndustry() {
		return basicIndustry;
	}

	public void setBasicIndustry(String basicIndustry) {
		this.basicIndustry = basicIndustry;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public LocalDate getDetailsDate() {
		return detailsDate;
	}

	public void setDetailsDate(LocalDate detailsDate) {
		this.detailsDate = detailsDate;
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

	public TickerDetails(String isin, String macro, String sector, String industry, String basicIndustry, String ticker,
			LocalDate detailsDate, double totalTradedVolume, double totalTradedValue, double totalMarketCap,
			double ffmc, double impactCost, double cmDailyVolatility, double cmAnnualVolatility) {
		super();
		this.isin = isin;
		this.macro = macro;
		this.sector = sector;
		this.industry = industry;
		this.basicIndustry = basicIndustry;
		this.ticker = ticker;
		this.detailsDate = detailsDate;
		this.totalTradedVolume = totalTradedVolume;
		this.totalTradedValue = totalTradedValue;
		this.totalMarketCap = totalMarketCap;
		this.ffmc = ffmc;
		this.impactCost = impactCost;
		this.cmDailyVolatility = cmDailyVolatility;
		this.cmAnnualVolatility = cmAnnualVolatility;
	}

	public TickerDetails() {
		super();
	}

}
