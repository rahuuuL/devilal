package com.terminal_devilal.indicators.atr.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.indicators.atr.entities.AverageTrueRangeEntity;
import com.terminal_devilal.indicators.atr.service.AverageTrueRangeService;

@RestController
@RequestMapping("/api/devilal/atr")
public class ATRController {
	private final AverageTrueRangeService atrService;

	public ATRController(AverageTrueRangeService atrService) {
		this.atrService = atrService;
	}

	@GetMapping("/ticker/from")
	public List<AverageTrueRangeEntity> getATRForStockFromDate(@RequestParam("ticker") String ticker,
			@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
		return atrService.getATRForTickerFromDate(ticker, fromDate);
	}
}
