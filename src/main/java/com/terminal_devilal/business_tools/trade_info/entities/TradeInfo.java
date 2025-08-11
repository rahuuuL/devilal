package com.terminal_devilal.business_tools.trade_info.entities;

import java.time.LocalDate;
import java.util.Objects;

import com.terminal_devilal.indicators.common_entities.TickerDateId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(TickerDateId.class)
@Table(name = "tradeInfo")
public class TradeInfo {

	@Id
	@Column(name = "ticker")
	private String ticker;

	@Id
	@Column(name = "date")
	private LocalDate date;

	@Column(name = "total_traded_volume")
	private double totalTradedVolume;

	@Column(name = "total_traded_value")
	private double totalTradedValue;

	@Column(name = "total_market_cap")
	private double totalMarketCap;

	@Column(name = "ffmc")
	private double ffmc;

	@Column(name = "impact_cost")
	private double impactCost;

	@Column(name = "daily_volatility")
	private double cmDailyVolatility;

	@Column(name = "annual_volatility")
	private double cmAnnualVolatility;

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
		return "TradeInfo [ticker=" + ticker + ", date=" + date + ", totalTradedVolume=" + totalTradedVolume
				+ ", totalTradedValue=" + totalTradedValue + ", totalMarketCap=" + totalMarketCap + ", ffmc=" + ffmc
				+ ", impactCost=" + impactCost + ", cmDailyVolatility=" + cmDailyVolatility + ", cmAnnualVolatility="
				+ cmAnnualVolatility + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(cmAnnualVolatility, cmDailyVolatility, date, ffmc, impactCost, ticker, totalMarketCap,
				totalTradedValue, totalTradedVolume);
	}

	public TradeInfo() {
		this.ticker = "";
		this.date = LocalDate.MIN;
		this.totalTradedVolume = 0.0;
		this.totalTradedValue = 0.0;
		this.totalMarketCap = 0.0;
		this.ffmc = 0.0;
		this.impactCost = 0.0;
		this.cmDailyVolatility = 0.0;
		this.cmAnnualVolatility = 0.0;
	}

}
