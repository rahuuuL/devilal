package com.terminal_devilal.business_tools.heatmap.dto;

import java.time.LocalDate;

import jakarta.persistence.Column;

public class HeatMapDTO {

	private String ticker;

	private double open;

	private double close;

	private double percentChange;

	private LocalDate tradeInfoDate;

	private double totalTradedVolume;

	private double totalTradedValue;

	private double totalMarketCap;

	private double ffmc;

	private double impactCost;

	private double cmDailyVolatility;

	private double cmAnnualVolatility;

	private String companyName;

	private String isin;

	private String macro;

	private String sector;

	private String industry;

	@Column(name = "basic_industry")
	private String basicIndustry;

	public HeatMapDTO() {
		super();
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public double getOpen() {
		return open;
	}

	public void setOpen(double open) {
		this.open = open;
	}

	public double getClose() {
		return close;
	}

	public void setClose(double close) {
		this.close = close;
	}

	public double getPercentChange() {
		return percentChange;
	}

	public void setPercentChange(double percentChange) {
		this.percentChange = percentChange;
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

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

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

}
