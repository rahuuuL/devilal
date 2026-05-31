package com.terminal_devilal.business_tools.ratio_analysis.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.business_tools.ratio_analysis.dto.RatioTImeSeries;
import com.terminal_devilal.business_tools.ratio_analysis.service.SharpeRatioService;

@RestController
@RequestMapping("/api/devilal/ratios")
public class SharpeRatioController {

	private final SharpeRatioService sharpeRatioService;

	public SharpeRatioController(SharpeRatioService sharpeRatioService) {
		super();
		this.sharpeRatioService = sharpeRatioService;
	}

	@GetMapping("/rolling-sharpe-sortino")
	public List<RatioTImeSeries> getRatioTimeSeries(@RequestParam(name = "tickers") List<String> tickers,

			@RequestParam(name = "fromDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

			@RequestParam(name = "toDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

			@RequestParam(name = "riskFreeRate", defaultValue = "0.06") double riskFreeRate,

			@RequestParam(name = "window", defaultValue = "20") int window) {
		return sharpeRatioService.computeRatiosForTimeFrame(tickers, fromDate, toDate, riskFreeRate, window);
	}

}
