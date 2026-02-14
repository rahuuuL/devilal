package com.terminal_devilal.core_processes.ticker_details.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.core_processes.ticker_details.model.TickerDetailsResponse;
import com.terminal_devilal.core_processes.ticker_details.service.TickerDetailsService;

@RestController
@RequestMapping("/api/devilal/ticker-info")
public class TickerDetailsController {

	private TickerDetailsService tickerDetailsService;

	TickerDetailsController(TickerDetailsService tickerDetailsService) {
		this.tickerDetailsService = tickerDetailsService;
	}

	@GetMapping("/details")
	public ResponseEntity<Map<String, TickerDetailsResponse>> getAllStockDetails() {
		return ResponseEntity.ok(tickerDetailsService.getTickerDetails());
	}
}
