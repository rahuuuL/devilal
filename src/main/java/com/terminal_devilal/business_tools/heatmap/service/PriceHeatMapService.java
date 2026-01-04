package com.terminal_devilal.business_tools.heatmap.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.heatmap.dto.HeatMapDTO;
import com.terminal_devilal.business_tools.heatmap.entities.PriceHeatMapProjection;
import com.terminal_devilal.business_tools.heatmap.repository.PriceHeatMapRepository;
import com.terminal_devilal.business_tools.trade_info.entities.TradeInfo;
import com.terminal_devilal.business_tools.trade_info.service.TradeInfoService;
import com.terminal_devilal.core_processes.sync_data.entity.TickerIndustryInfo;
import com.terminal_devilal.core_processes.sync_data.repository.TickerIndustryInfoRepository;

@Service
public class PriceHeatMapService {

	private final PriceHeatMapRepository priceHeatMapRepository;

	private final TickerIndustryInfoRepository tickerIndustryInfoRepository;

	private final TradeInfoService infoService;

	public PriceHeatMapService(PriceHeatMapRepository priceHeatMapRepository,
			TickerIndustryInfoRepository tickerIndustryInfoRepository, TradeInfoService infoService) {
		super();
		this.priceHeatMapRepository = priceHeatMapRepository;
		this.tickerIndustryInfoRepository = tickerIndustryInfoRepository;
		this.infoService = infoService;
	}

	public List<HeatMapDTO> getPriceHeatMap(LocalDate fromDate, LocalDate toDate) {

		List<PriceHeatMapProjection> priceChangeData = priceHeatMapRepository.getHeatMapData(fromDate, toDate);

		Map<String, TradeInfo> tradeInfoMap = infoService.getTradeInfoData();

		// Fetch all industry data once
		Map<String, TickerIndustryInfo> industryMap = tickerIndustryInfoRepository.findAll().stream()
				.collect(Collectors.toMap(TickerIndustryInfo::getTicker, e -> e, (a, b) -> a));

		List<HeatMapDTO> dtoList = new ArrayList<>();

		for (PriceHeatMapProjection result : priceChangeData) {

			String ticker = result.getTicker();

			TradeInfo tradeInfo = tradeInfoMap.get(ticker);
			TickerIndustryInfo industryInfo = industryMap.get(ticker);

			HeatMapDTO dto = new HeatMapDTO();

			// ===== Price data =====
			dto.setTicker(ticker);
			dto.setOpen(result.getOpen() != null ? result.getOpen() : 0.0);
			dto.setClose(result.getClose() != null ? result.getClose() : 0.0);
			dto.setPercentChange(result.getPercentChange() != null ? result.getPercentChange() : 0.0);

			// ===== Trade info =====
			dto.setTradeInfoDate(tradeInfo != null ? tradeInfo.getDate() : null);
			dto.setTotalTradedVolume(tradeInfo != null ? tradeInfo.getTotalTradedVolume() : 0.0);
			dto.setTotalTradedValue(tradeInfo != null ? tradeInfo.getTotalTradedValue() : 0.0);
			dto.setTotalMarketCap(tradeInfo != null ? tradeInfo.getTotalMarketCap() : 0.0);
			dto.setFfmc(tradeInfo != null ? tradeInfo.getFfmc() : 0.0);
			dto.setImpactCost(tradeInfo != null ? tradeInfo.getImpactCost() : 0.0);
			dto.setCmDailyVolatility(tradeInfo != null ? tradeInfo.getCmDailyVolatility() : 0.0);
			dto.setCmAnnualVolatility(tradeInfo != null ? tradeInfo.getCmAnnualVolatility() : 0.0);

			// ===== Industry info =====
			if (industryInfo != null) {
				dto.setMacro(industryInfo.getMacro());
				dto.setSector(industryInfo.getSector());
				dto.setIndustry(industryInfo.getIndustry());
				dto.setBasicIndustry(industryInfo.getBasicIndustry());
			}

			dtoList.add(dto);
		}

		return dtoList.stream()
				.sorted(Comparator.comparingDouble(HeatMapDTO::getPercentChange).reversed()
						.thenComparing(Comparator.comparingDouble(HeatMapDTO::getTotalMarketCap).reversed())
						.thenComparingDouble(HeatMapDTO::getCmDailyVolatility))
				.collect(Collectors.toList());
	}
}
