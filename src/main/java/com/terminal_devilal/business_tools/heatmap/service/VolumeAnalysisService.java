package com.terminal_devilal.business_tools.heatmap.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.heatmap.dto.VolumeAnalysisDTO;
import com.terminal_devilal.business_tools.heatmap.entities.VolumeAnalysisProjection;
import com.terminal_devilal.business_tools.heatmap.repository.VolumeAnalysisRepository;
import com.terminal_devilal.business_tools.trade_info.entities.TradeInfo;
import com.terminal_devilal.business_tools.trade_info.service.TradeInfoService;

@Service
public class VolumeAnalysisService {

	private final VolumeAnalysisRepository volumeAnalysisRepository;

	private final TradeInfoService infoService;

	public VolumeAnalysisService(VolumeAnalysisRepository volumeAnalysisRepository, TradeInfoService infoService) {
		super();
		this.volumeAnalysisRepository = volumeAnalysisRepository;
		this.infoService = infoService;
	}

	public List<VolumeAnalysisDTO> getVolumeAnalysis(LocalDate fromDate, LocalDate toDate) {
		List<VolumeAnalysisProjection> volumeData = this.volumeAnalysisRepository.findVolumeAnalysis(fromDate, toDate);

		Map<String, TradeInfo> tradeInfoMap = this.infoService.getTradeInfoData();

		List<VolumeAnalysisDTO> dtoList = new ArrayList<>();

		for (VolumeAnalysisProjection result : volumeData) {
			String ticker = result.getTicker();

			TradeInfo tradeInfo = tradeInfoMap.getOrDefault(ticker, null);

			VolumeAnalysisDTO dto = new VolumeAnalysisDTO();
			dto.setTicker(ticker);
		    dto.setOccurrenceDate(result.getDate());
		    dto.setVolumeCameIn(result.getVolume() != null ? result.getVolume() : 0.0);
		    dto.setAverageVolume(result.getAvgVolume() != null ? result.getAvgVolume() : 0.0);
		    dto.setTimes(result.getTimes() != null ? result.getTimes() : 0.0);
		    dto.setDeliveryPercentage(result.getDeliveryPercentage() != null ? result.getDeliveryPercentage() : 0.0);


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
				.sorted(Comparator.comparing(VolumeAnalysisDTO::getOccurrenceDate)
						.thenComparing(Comparator.comparingDouble(VolumeAnalysisDTO::getTimes))
						.thenComparing(Comparator.comparing(VolumeAnalysisDTO::getOccurrenceDate).reversed())
						.thenComparingDouble(VolumeAnalysisDTO::getTotalMarketCap).reversed())
				.collect(Collectors.toList());

		return dtoList;
	}

}
