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

@Service
public class PriceHeatMapService {

	private final PriceHeatMapRepository priceHeatMapRepository;

	private final TradeInfoService infoService;

	public PriceHeatMapService(PriceHeatMapRepository priceHeatMapRepository, TradeInfoService infoService) {
		super();
		this.priceHeatMapRepository = priceHeatMapRepository;
		this.infoService = infoService;
	}

	public List<HeatMapDTO> getPriceHeatMap(LocalDate fromDate, LocalDate toDate) {
		List<PriceHeatMapProjection> priceChangeData = priceHeatMapRepository.getHeatMapData(fromDate, toDate);
		Map<String, TradeInfo> tradeInfoMap = this.infoService.getTradeInfoData();

		List<HeatMapDTO> dtoList = new ArrayList<>();

		for (PriceHeatMapProjection result : priceChangeData) {
			String ticker = result.getTicker();
			TradeInfo tradeInfo = tradeInfoMap.getOrDefault(ticker, null);

			HeatMapDTO dto = new HeatMapDTO();

			dto.setTicker(ticker);

			// Values from HeatMapProjection
			dto.setTicker(ticker);
			dto.setOpen(result.getOpen() != null ? result.getOpen() : 0.0);
			dto.setClose(result.getClose() != null ? result.getClose() : 0.0);
			dto.setPercentChange(result.getPercentChange() != null ? result.getPercentChange() : 0.0);

			// Values from TradeInfo
			dto.setTradeInfoDate(tradeInfo != null ? tradeInfo.getDate() : null);
			dto.setTotalTradedVolume(tradeInfo != null ? tradeInfo.getTotalTradedVolume() : 0.0);
			dto.setTotalTradedValue(tradeInfo != null ? tradeInfo.getTotalTradedValue() : 0.0);
			dto.setTotalMarketCap(tradeInfo != null ? tradeInfo.getTotalMarketCap() : 0.0);
			dto.setFfmc(tradeInfo != null ? tradeInfo.getFfmc() : 0.0);
			dto.setImpactCost(tradeInfo != null ? tradeInfo.getImpactCost() : 0.0);
			dto.setCmDailyVolatility(tradeInfo != null ? tradeInfo.getCmDailyVolatility() : 0.0);
			dto.setCmAnnualVolatility(tradeInfo != null ? tradeInfo.getCmAnnualVolatility() : 0.0);

			dtoList.add(dto);
		}
		dtoList = dtoList.stream()
				.sorted(Comparator.comparingDouble(HeatMapDTO::getPercentChange).reversed()
						.thenComparing(Comparator.comparingDouble(HeatMapDTO::getTotalMarketCap).reversed())
						.thenComparingDouble(HeatMapDTO::getCmDailyVolatility))
				.collect(Collectors.toList());
		return dtoList;
	}

}
