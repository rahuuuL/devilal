package com.terminal_devilal.indicators.atr.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.indicators.atr.entities.projections.TrueRangeProjection;
import com.terminal_devilal.indicators.atr.service.AverageTrueRangeService;

@RestController
@RequestMapping("/api/devilal/atr")
public class ATRController {
	private final AverageTrueRangeService atrService;

	public ATRController(AverageTrueRangeService atrService) {
		this.atrService = atrService;
	}

	@GetMapping("/last-n")
	public List<TrueRangeProjection> getATRForStockFromDate(@RequestParam("tickers") List<String> tickers,
			@RequestParam(value = "n", required = true) int n) {
		return atrService.getLastNRecordsPerTicker(tickers, n);
	}
}
