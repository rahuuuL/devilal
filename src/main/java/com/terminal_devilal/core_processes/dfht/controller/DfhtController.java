package com.terminal_devilal.core_processes.dfht.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
