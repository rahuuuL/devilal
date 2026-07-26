package com.terminal_devilal.core_processes.dfht.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.core_processes.dfht.entity.DataFetchEntity;
import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;

@RestController
@RequestMapping("/api/devilal/dfht")
public class DfhtController {

	private final DataFetchHistoryService dataFetchHistoryService;

	public DfhtController(DataFetchHistoryService dataFetchHistoryService) {
		super();
		this.dataFetchHistoryService = dataFetchHistoryService;
	}

	@GetMapping("/list-tickers")
	public ResponseEntity<List<String>> getListOfTickers() {
		return ResponseEntity.ok(dataFetchHistoryService.getAllTickers());
	}

	@GetMapping("/records")
	public ResponseEntity<List<DataFetchEntity>> getAllDfhtRecords() {
		return ResponseEntity.ok(dataFetchHistoryService.getProcessedDatesForTickers());
	}

	@PutMapping("/{ticker}/pdvt-last-date")
	public ResponseEntity<Map<String, String>> updatePdvtLastDate(
			@PathVariable String ticker,
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		dataFetchHistoryService.updateLastDateForPdvt(ticker, date);
		return ResponseEntity.ok(Map.of(
				"message", "DFHT last fetch date updated",
				"ticker", ticker,
				"pdvtLastDate", date.toString()));
	}

}
