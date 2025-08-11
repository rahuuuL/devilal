package com.terminal_devilal.indicators.vwap.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.indicators.vwap.dto.VWAPDTO;
import com.terminal_devilal.indicators.vwap.entities.VWAPEntity;
import com.terminal_devilal.indicators.vwap.service.VWAPService;

@RestController
@RequestMapping("/api/devilal/vwap")
public class VWAPController {
	private final VWAPService vwapService;

	public VWAPController(VWAPService vwapService) {
		this.vwapService = vwapService;
	}

	@GetMapping("/from")
	public Map<String, List<VWAPEntity>> getVwapDataFromDate(
			@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
		return vwapService.getVwapDataFromDate(fromDate);
	}

	@GetMapping("/ticker/from")
	public List<VWAPDTO> getVWAPForTickerFromDate(@RequestParam("ticker") String ticker,
			@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
		return vwapService.getVWAPForTickerFromDate(ticker, fromDate).stream()
				.map(v -> new VWAPDTO(v.getDate(), v.getClosePrice(), v.getVwap(), v.getVwapProximity())).toList();
	}

}
