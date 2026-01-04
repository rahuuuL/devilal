package com.terminal_devilal.core_processes.sync_data.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.core_processes.sync_data.model.DataSyncProcessResponse;
import com.terminal_devilal.core_processes.sync_data.service.DataSync;
import com.terminal_devilal.core_processes.sync_data.service.TickerIndustryInfoUpdate;

@RestController
@RequestMapping("/pdv")
public class DeliveryPriceVolumeController {
	private DataSync dataSync;

	private final TickerIndustryInfoUpdate service;

	public DeliveryPriceVolumeController(DataSync dataSync, TickerIndustryInfoUpdate service) {
		super();
		this.dataSync = dataSync;
		this.service = service;
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
}