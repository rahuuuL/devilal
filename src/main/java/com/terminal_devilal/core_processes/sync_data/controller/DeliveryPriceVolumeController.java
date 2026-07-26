package com.terminal_devilal.core_processes.sync_data.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.core_processes.sync_data.model.DataSyncProcessResponse;
import com.terminal_devilal.core_processes.sync_data.service.DataSync;
import com.terminal_devilal.core_processes.sync_data.service.TickerIndustryInfoUpdate;
import com.terminal_devilal.core_processes.sync_data.service.TickerTradeInfoUpdate;

@RestController
@RequestMapping("/pdv")
public class DeliveryPriceVolumeController {
	private DataSync dataSync;

	private final TickerIndustryInfoUpdate service;
	private final TickerTradeInfoUpdate tradeInfoService;

	public DeliveryPriceVolumeController(DataSync dataSync, TickerIndustryInfoUpdate service,
			TickerTradeInfoUpdate tradeInfoService) {
		super();
		this.dataSync = dataSync;
		this.service = service;
		this.tradeInfoService = tradeInfoService;
	}

	@GetMapping("/revise-data")
	public ResponseEntity<DataSyncProcessResponse> processPdvDataTillDate() {
		DataSyncProcessResponse res = new DataSyncProcessResponse("Data Sync Process Started Please wait", true);
		this.dataSync.processPdvDataTillDate();
		return ResponseEntity.ok(res);
	}

	@GetMapping("/sync/company-industry")
	public ResponseEntity<String> syncCompanyIndustry() {
		service.updateCompanyIndustryData();
		return ResponseEntity.ok("NSE company-industry sync started");
	}

	@GetMapping("/sync/trade-info")
	public ResponseEntity<String> syncTradeInfo() {
		tradeInfoService.updateTradeInfoData();
		return ResponseEntity.ok("NSE trade-info sync started");
	}
}