package com.terminal_devilal.business_tools.heatmap.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.business_tools.heatmap.dto.HeatMapDTO;
import com.terminal_devilal.business_tools.heatmap.dto.VolumeAnalysisDTO;
import com.terminal_devilal.business_tools.heatmap.service.PriceHeatMapService;
import com.terminal_devilal.business_tools.heatmap.service.VolumeAnalysisService;

@RestController
@RequestMapping("/api/devilal")
public class HeatMapController {

	private final PriceHeatMapService priceHeatMapService;

	private final VolumeAnalysisService analysisService;

	public HeatMapController(PriceHeatMapService priceHeatMapService, VolumeAnalysisService analysisService) {
		super();
		this.priceHeatMapService = priceHeatMapService;
		this.analysisService = analysisService;
	}

	/**
	 * Get stock percent changes between two dates. Example: GET
	 * /api/devilal/heatmap?fromDate=2024-02-01&toDate=2024-02-10
	 */
	@GetMapping("/heatmap")
	public List<HeatMapDTO> getHeatMap(
			@RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

		return priceHeatMapService.getPriceHeatMap(fromDate, toDate);
	}

	@GetMapping("/volumeAnalysis")
	public List<VolumeAnalysisDTO> getVolumeAnalysis(
			@RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

		return this.analysisService.getVolumeAnalysis(fromDate, toDate);

	}
}
