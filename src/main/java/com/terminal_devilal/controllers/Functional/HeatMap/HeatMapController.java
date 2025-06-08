package com.terminal_devilal.controllers.Functional.HeatMap;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.controllers.Functional.HeatMap.DAO.HeatMapRepository;
import com.terminal_devilal.controllers.Functional.HeatMap.DAO.VolumeAnalysisRepository;
import com.terminal_devilal.controllers.Functional.HeatMap.Model.HeatMapDTO;
import com.terminal_devilal.controllers.Functional.HeatMap.Model.VolumeAnalysisDTO;

@RestController
@RequestMapping("/api/devilal")
public class HeatMapController {

	private final HeatMapRepository heatMapRepository;

	private final VolumeAnalysisRepository volumeAnalysisRepository;

	public HeatMapController(HeatMapRepository heatMapRepository, VolumeAnalysisRepository volumeAnalysisRepository) {
		super();
		this.heatMapRepository = heatMapRepository;
		this.volumeAnalysisRepository = volumeAnalysisRepository;
	}

	/**
	 * Get stock percent changes between two dates. Example: GET
	 * /api/devilal/heatmap?fromDate=2024-02-01&toDate=2024-02-10
	 */
	@GetMapping("/heatmap")
	public List<HeatMapDTO> getHeatMap(
			@RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

		return heatMapRepository.findPriceChangesBetweenDates(fromDate, toDate);
	}

	@GetMapping("/volumeAnalysis")
	public List<VolumeAnalysisDTO> getVolumeAnalysis(
			@RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

		List<Object[]> results = volumeAnalysisRepository.findVolumeAnalysis(fromDate, toDate);
		
		return results.stream()
				.map(obj -> new VolumeAnalysisDTO((String) obj[0], ((java.sql.Date) obj[1]).toLocalDate(),
						((Number) obj[2]).longValue(), ((Number) obj[3]).doubleValue(), ((Number) obj[4]).doubleValue(),
						((Number) obj[5]).doubleValue()))
				.collect(Collectors.toList());

	}
}
