package com.terminal_devilal.controllers.Functional.HeatMap.Model;

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
	

	public VolumeAnalysisDTO() {
		super();
	}

	public VolumeAnalysisDTO(String ticker, LocalDate occurrenceDate, double volumeCameIn, double averageVolume,
			double times, double deliveryPercentage, LocalDate tradeInfoDate, double totalTradedVolume,
			double totalTradedValue, double totalMarketCap, double ffmc, double impactCost, double cmDailyVolatility,
			double cmAnnualVolatility) {
		super();
		this.ticker = ticker;
		this.occurrenceDate = occurrenceDate;
		this.volumeCameIn = volumeCameIn;
		this.averageVolume = averageVolume;
		this.times = times;
		this.deliveryPercentage = deliveryPercentage;
		this.tradeInfoDate = tradeInfoDate;
		this.totalTradedVolume = totalTradedVolume;
		this.totalTradedValue = totalTradedValue;
		this.totalMarketCap = totalMarketCap;
		this.ffmc = ffmc;
		this.impactCost = impactCost;
		this.cmDailyVolatility = cmDailyVolatility;
		this.cmAnnualVolatility = cmAnnualVolatility;
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

}
