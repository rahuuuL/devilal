package com.terminal_devilal.business_tools.heatmap.dto;

import java.time.LocalDate;

public class VolumeAnalysisDTO {

	private String ticker;

	private LocalDate occurrenceDate;

	private double volumeCameIn;

	private double averageVolume;

	private double times;

	private double deliveryPercentage;

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

	private String basicIndustry;

	public VolumeAnalysisDTO() {
		super();
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public LocalDate getOccurrenceDate() {
		return occurrenceDate;
	}

	public void setOccurrenceDate(LocalDate occurrenceDate) {
		this.occurrenceDate = occurrenceDate;
	}

	public double getVolumeCameIn() {
		return volumeCameIn;
	}

	public void setVolumeCameIn(double volumeCameIn) {
		this.volumeCameIn = volumeCameIn;
	}

	public double getAverageVolume() {
		return averageVolume;
	}

	public void setAverageVolume(double averageVolume) {
		this.averageVolume = averageVolume;
	}

	public double getTimes() {
		return times;
	}

	public void setTimes(double times) {
		this.times = times;
	}

	public double getDeliveryPercentage() {
		return deliveryPercentage;
	}

	public void setDeliveryPercentage(double deliveryPercentage) {
		this.deliveryPercentage = deliveryPercentage;
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
