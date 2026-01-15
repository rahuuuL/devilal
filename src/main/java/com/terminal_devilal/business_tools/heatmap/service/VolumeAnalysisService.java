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
import com.terminal_devilal.core_processes.sync_data.entity.TickerIndustryInfo;
import com.terminal_devilal.core_processes.sync_data.repository.TickerIndustryInfoRepository;

@Service
public class VolumeAnalysisService {

	private final VolumeAnalysisRepository volumeAnalysisRepository;

	private final TradeInfoService infoService;

	private final TickerIndustryInfoRepository tickerIndustryInfoRepository;

	public VolumeAnalysisService(VolumeAnalysisRepository volumeAnalysisRepository, TradeInfoService infoService,
			TickerIndustryInfoRepository tickerIndustryInfoRepository) {
		super();
		this.volumeAnalysisRepository = volumeAnalysisRepository;
		this.infoService = infoService;
		this.tickerIndustryInfoRepository = tickerIndustryInfoRepository;
	}

	public List<VolumeAnalysisDTO> getVolumeAnalysis(LocalDate fromDate, LocalDate toDate) {
		List<VolumeAnalysisProjection> volumeData = this.volumeAnalysisRepository.findVolumeAnalysis(fromDate, toDate);

		// Fetch all industry data once
		Map<String, TickerIndustryInfo> industryMap = tickerIndustryInfoRepository.findAll().stream()
				.collect(Collectors.toMap(TickerIndustryInfo::getTicker, e -> e, (a, b) -> a));

		Map<String, TradeInfo> tradeInfoMap = this.infoService.getTradeInfoData();

		List<VolumeAnalysisDTO> dtoList = new ArrayList<>();

		for (VolumeAnalysisProjection result : volumeData) {
			String ticker = result.getTicker();

			TradeInfo tradeInfo = tradeInfoMap.getOrDefault(ticker, null);

			TickerIndustryInfo industryInfo = industryMap.get(ticker);

			VolumeAnalysisDTO dto = new VolumeAnalysisDTO();
			// ===== Volume Data =====
			dto.setTicker(ticker);
			dto.setOccurrenceDate(result.getDate());
			dto.setVolumeCameIn(result.getVolume() != null ? result.getVolume() : 0.0);
			dto.setAverageVolume(result.getAvgVolume() != null ? result.getAvgVolume() : 0.0);
			dto.setTimes(result.getTimes() != null ? result.getTimes() : 0.0);

			dto.setDeliveryPercentage(result.getDeliveryPercentage() != null ? result.getDeliveryPercentage() : 0.0);

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
				dto.setIsin(industryInfo.getIsin());
				dto.setCompanyName(industryInfo.getCompanyName());
			}

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
