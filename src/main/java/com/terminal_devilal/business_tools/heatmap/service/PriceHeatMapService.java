package com.terminal_devilal.business_tools.heatmap.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.heatmap.dto.HeatMapDTO;
import com.terminal_devilal.business_tools.heatmap.entities.PriceHeatMapProjection;
import com.terminal_devilal.business_tools.heatmap.repository.PriceHeatMapRepository;
import com.terminal_devilal.business_tools.trade_info.service.TradeInfoService;
import com.terminal_devilal.core_processes.sync_data.repository.TickerIndustryInfoRepository;

@Service
public class PriceHeatMapService {

	private final PriceHeatMapRepository priceHeatMapRepository;

	public PriceHeatMapService(PriceHeatMapRepository priceHeatMapRepository,
			TickerIndustryInfoRepository tickerIndustryInfoRepository, TradeInfoService infoService) {
		super();
		this.priceHeatMapRepository = priceHeatMapRepository;
	}

	public List<HeatMapDTO> getPriceHeatMap(LocalDate fromDate, LocalDate toDate) {

		List<PriceHeatMapProjection> priceChangeData = priceHeatMapRepository.getHeatMapData(fromDate, toDate);

		List<HeatMapDTO> dtoList = new ArrayList<>();

		for (PriceHeatMapProjection result : priceChangeData) {

			String ticker = result.getTicker();

			HeatMapDTO dto = new HeatMapDTO();

			// ===== Price data =====
			dto.setTicker(ticker);
			dto.setOpen(result.getOpen() != null ? result.getOpen() : 0.0);
			dto.setClose(result.getClose() != null ? result.getClose() : 0.0);
			dto.setPercentChange(result.getPercentChange() != null ? result.getPercentChange() : 0.0);

			dtoList.add(dto);
		}

		return dtoList;
	}
}
