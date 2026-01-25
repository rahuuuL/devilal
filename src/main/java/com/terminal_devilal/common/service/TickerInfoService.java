package com.terminal_devilal.common.service;

import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.trade_info.repository.TradeInfoRepository;
import com.terminal_devilal.common.model.TickerDetails;
import com.terminal_devilal.core_processes.sync_data.repository.TickerIndustryInfoRepository;

@Service
public class TickerInfoService {
	private final TickerIndustryInfoRepository sectorDetails;

	private final TradeInfoRepository infoRepository;

	public TickerInfoService(TickerIndustryInfoRepository sectorDetails, TradeInfoRepository infoRepository) {
		super();
		this.sectorDetails = sectorDetails;
		this.infoRepository = infoRepository;
	}

	/**
	 * Enrich any TickerDetails subclass with sector + trade info
	 */
	public <T extends TickerDetails> T enrichTickerDetails(String ticker, T target) {

		// ---- Trade info ----
		infoRepository.findFirstByTickerOrderByDateDesc(ticker).ifPresentOrElse(trade -> {
			target.setDetailsDate(trade.getDate());
			target.setTotalTradedVolume(trade.getTotalTradedVolume());
			target.setTotalTradedValue(trade.getTotalTradedValue());
			target.setTotalMarketCap(trade.getTotalMarketCap());
			target.setFfmc(trade.getFfmc());
			target.setImpactCost(trade.getImpactCost());
			target.setCmDailyVolatility(trade.getCmDailyVolatility());
			target.setCmAnnualVolatility(trade.getCmAnnualVolatility());
		}, () -> {
			target.setDetailsDate(null);
			target.setTotalTradedVolume(0.0);
			target.setTotalTradedValue(0.0);
			target.setTotalMarketCap(0.0);
			target.setFfmc(0.0);
			target.setImpactCost(0.0);
			target.setCmDailyVolatility(0.0);
			target.setCmAnnualVolatility(0.0);
		});

		// ---- Sector info ----
		sectorDetails.findById(ticker).ifPresentOrElse(sector -> {
			target.setIsin(sector.getIsin());
			target.setMacro(sector.getMacro());
			target.setSector(sector.getSector());
			target.setIndustry(sector.getIndustry());
			target.setBasicIndustry(sector.getBasicIndustry());
		}, () -> {
			target.setIsin("");
			target.setMacro("");
			target.setSector("");
			target.setIndustry("");
			target.setBasicIndustry("");
		});

		return target;
	}

}
