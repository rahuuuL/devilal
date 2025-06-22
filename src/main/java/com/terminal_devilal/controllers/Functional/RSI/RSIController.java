package com.terminal_devilal.controllers.Functional.RSI;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.controllers.DataGathering.Model.RSI;
import com.terminal_devilal.controllers.DataGathering.Service.RSIService;

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
	public List<RSI> getAllRSIByDate(
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return rsiService.getAllRSIByDate(date);
	}

	// RSI for a stock in a price range
	@GetMapping("/from")
	public List<RSI> getRSIFromDate(@RequestParam("ticker") String ticker,
			@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
		return rsiService.getRSIFromDate(ticker, fromDate);
	}

}
