package com.terminal_devilal.indicators.rsi.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.indicators.rsi.dto.RsiPercentileDTO;
import com.terminal_devilal.indicators.rsi.entity.RSIEntity;
import com.terminal_devilal.indicators.rsi.entity.projections.RsiProjection;
import com.terminal_devilal.indicators.rsi.service.RSIService;

@RestController
@RequestMapping("/api/devilal/rsi")
public class RSIController {

	private final RSIService rsiService;

	public RSIController(RSIService rsiService) {
		super();
		this.rsiService = rsiService;
	}

	// RSI for all stocks for a date
	@GetMapping("/all")
	public List<RSIEntity> getAllRSIByDate(
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return rsiService.getAllRSIByDate(date);
	}

	// RSI for a stock in a price range
	@GetMapping("/tickers/within-dates")
	public List<RsiProjection> getRSIWithinDatesForTickers(
			@RequestParam(value = "fromDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(value = "toDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam("tickers") List<String> tickers) {
		return rsiService.getRSIWithinDatesForTickers(tickers, fromDate, toDate);
	}

	@GetMapping("/percentile")
	public List<RsiPercentileDTO> getRsiPercentiles(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate,
			@RequestParam(defaultValue = "true") boolean rsi14) {
		return rsiService.computeRsiPercentiles(fromDate, toDate, rsi14);
	}

}
