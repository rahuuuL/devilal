package com.terminal_devilal.business_tools.heatmap.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.heatmap.dto.VolumeAnalysisDTO;
import com.terminal_devilal.business_tools.heatmap.entities.VolumeAnalysisProjection;
import com.terminal_devilal.business_tools.heatmap.repository.VolumeAnalysisRepository;
import com.terminal_devilal.business_tools.trade_info.service.TradeInfoService;
import com.terminal_devilal.core_processes.sync_data.repository.TickerIndustryInfoRepository;

@Service
public class VolumeAnalysisService {

	private final VolumeAnalysisRepository volumeAnalysisRepository;

	public VolumeAnalysisService(VolumeAnalysisRepository volumeAnalysisRepository, TradeInfoService infoService,
			TickerIndustryInfoRepository tickerIndustryInfoRepository) {
		super();
		this.volumeAnalysisRepository = volumeAnalysisRepository;
	}

	public List<VolumeAnalysisDTO> getVolumeAnalysis(LocalDate fromDate, LocalDate toDate) {
		List<VolumeAnalysisProjection> volumeData = this.volumeAnalysisRepository.findVolumeAnalysis(fromDate, toDate);

		List<VolumeAnalysisDTO> dtoList = new ArrayList<>();

		for (VolumeAnalysisProjection result : volumeData) {
			String ticker = result.getTicker();

			VolumeAnalysisDTO dto = new VolumeAnalysisDTO();
			// ===== Volume Data =====
			dto.setTicker(ticker);
			dto.setOccurrenceDate(result.getDate());
			dto.setVolumeCameIn(result.getVolume() != null ? result.getVolume() : 0.0);
			dto.setAverageVolume(result.getAvgVolume() != null ? result.getAvgVolume() : 0.0);
			dto.setTimes(result.getTimes() != null ? result.getTimes() : 0.0);

			dto.setDeliveryPercentage(result.getDeliveryPercentage() != null ? result.getDeliveryPercentage() : 0.0);

			dtoList.add(dto);
		}

		dtoList = dtoList.stream()
				.sorted(Comparator.comparing(VolumeAnalysisDTO::getOccurrenceDate)
						.thenComparing(Comparator.comparingDouble(VolumeAnalysisDTO::getTimes))
						.thenComparing(Comparator.comparing(VolumeAnalysisDTO::getOccurrenceDate).reversed()))
				.collect(Collectors.toList());

		return dtoList;
	}

}
