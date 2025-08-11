package com.terminal_devilal.business_tools.mannkendall.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallAPIResponse;
import com.terminal_devilal.business_tools.mannkendall.service.AnalyzeMannKendallForTicker;

@RestController
@RequestMapping("/api/devilal/mannKendallTrendAnalysis")
public class MannKendallTrendAnalysisController {

	private final AnalyzeMannKendallForTicker analyzeMannKendallForTicker;

	@Autowired
	public MannKendallTrendAnalysisController(AnalyzeMannKendallForTicker analyzeMannKendallForTicker) {
		this.analyzeMannKendallForTicker = analyzeMannKendallForTicker;
	}

	/**
	 * Endpoint: GET /api/devilal/mannKendallTrendAnalysis?fromDate=2023-01-01
	 */
	@GetMapping("/all")
	public ResponseEntity<List<MannKendallAPIResponse>> getTrendAnalysis(
			@RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(name = "riskFreeRate", defaultValue = "0.06") double riskFreeRate,
			@RequestParam("column") String columnName) {

		List<MannKendallAPIResponse> results = analyzeMannKendallForTicker.getMannKendallTrendAnalysis(fromDate,
				columnName, riskFreeRate);
		return ResponseEntity.ok(results);
	}

	@GetMapping("/forTickers")
	public ResponseEntity<List<MannKendallAPIResponse>> getTrendAnalysis(
			@RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(name = "riskFreeRate", defaultValue = "0.06") double riskFreeRate,
			@RequestParam("column") String columnName, @RequestParam("tickers") List<String> tickers) {

		List<MannKendallAPIResponse> results = analyzeMannKendallForTicker.getMannKendallTrendAnalysis(fromDate,
				columnName, tickers, riskFreeRate);
		return ResponseEntity.ok(results);
	}

}
