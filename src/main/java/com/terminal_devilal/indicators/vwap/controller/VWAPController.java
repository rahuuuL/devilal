package com.terminal_devilal.indicators.vwap.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.indicators.vwap.entity.projections.VwapProjection;
import com.terminal_devilal.indicators.vwap.service.VWAPService;

@RestController
@RequestMapping("/api/devilal/vwap")
public class VWAPController {
	private final VWAPService vwapService;

	public VWAPController(VWAPService vwapService) {
		this.vwapService = vwapService;
	}

	@GetMapping("/within-dates")
	public List<VwapProjection> getVwapDataWithinDates(
			@RequestParam(value = "fromDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(value = "toDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam("tickers") List<String> tickers) {
		return vwapService.getVwapDataWithinDates(tickers, fromDate, toDate);
	}

}
