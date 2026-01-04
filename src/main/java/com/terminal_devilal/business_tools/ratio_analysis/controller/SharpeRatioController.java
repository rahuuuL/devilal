package com.terminal_devilal.business_tools.ratio_analysis.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.business_tools.ratio_analysis.dto.RatioTImeSeries;
import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeRatioDTO;
import com.terminal_devilal.business_tools.ratio_analysis.service.SharpeRatioService;

@RestController
@RequestMapping("/api/devilal")
public class SharpeRatioController {

	private final SharpeRatioService sharpeRatioService;

	public SharpeRatioController(SharpeRatioService sharpeRatioService) {
		super();
		this.sharpeRatioService = sharpeRatioService;
	}

	@GetMapping("/sharpe/all")
	public Map<String, SharpeRatioDTO> getSharpeRatios(@RequestParam(name = "days", defaultValue = "30") int days,
			@RequestParam(name = "riskFreeRate", defaultValue = "0.06") double riskFreeRate) {
		LocalDate from = LocalDate.now().minusDays(days);
		return sharpeRatioService.computeSharpeRatios(from, riskFreeRate);
	}

	@GetMapping("/sharpe")
	public Map<String, SharpeRatioDTO> getSharpeRatios(@RequestParam(name = "days", defaultValue = "30") int days,
			@RequestParam(name = "riskFreeRate", defaultValue = "0.06") double riskFreeRate,
			@RequestParam(name = "tickers", required = true) List<String> tickers) {
		LocalDate from = LocalDate.now().minusDays(days);
		return sharpeRatioService.computeSharpeRatios(from, riskFreeRate, tickers);
	}

	@GetMapping("/ratios/timeseries")
	public List<RatioTImeSeries> getRatioTimeSeries(@RequestParam(name = "tickers") List<String> tickers,

			@RequestParam(name = "fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

			@RequestParam(name = "toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

			@RequestParam(name = "riskFreeRate", defaultValue = "0.06") double riskFreeRate,

			@RequestParam(name = "window", defaultValue = "20") int window) {
		return sharpeRatioService.computeRatiosForTimeFrame(tickers, fromDate, toDate, riskFreeRate, window);
	}

}
