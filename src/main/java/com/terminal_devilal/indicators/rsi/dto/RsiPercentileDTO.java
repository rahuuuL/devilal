package com.terminal_devilal.indicators.rsi.dto;

import com.terminal_devilal.business_tools.trade_info.entities.TradeInfo;
import com.terminal_devilal.common.model.TickerDetails;
import com.terminal_devilal.core_processes.sync_data.entity.TickerIndustryInfo;

public class RsiPercentileDTO extends TickerDetails {

	public RsiPercentileDTO() {
		super();
	}

	public RsiPercentileDTO(TickerIndustryInfo sectorDetails, TradeInfo tradeInfo, String ticker, double rsiValue,
			double percentile) {
		super(sectorDetails.getIsin(), sectorDetails.getMacro(), sectorDetails.getSector(), sectorDetails.getIndustry(),
				sectorDetails.getBasicIndustry(), tradeInfo.getTicker(), tradeInfo.getDate(),
				tradeInfo.getTotalTradedVolume(), tradeInfo.getTotalTradedValue(), tradeInfo.getTotalMarketCap(),
				tradeInfo.getFfmc(), tradeInfo.getImpactCost(), tradeInfo.getCmDailyVolatility(),
				tradeInfo.getCmAnnualVolatility());
		this.ticker = ticker;
		this.rsiValue = rsiValue;
		this.percentile = percentile;
	}

	private String ticker;

	private double rsiValue;

	private double percentile;

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public double getRsiValue() {
		return rsiValue;
	}

	public void setRsiValue(double rsiValue) {
		this.rsiValue = rsiValue;
	}

	public double getPercentile() {
		return percentile;
	}

	public void setPercentile(double percentile) {
		this.percentile = percentile;
	}

}
