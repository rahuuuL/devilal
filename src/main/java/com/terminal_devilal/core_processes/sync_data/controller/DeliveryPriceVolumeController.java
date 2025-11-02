package com.terminal_devilal.core_processes.sync_data.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.core_processes.sync_data.model.DataSyncProcessResponse;
import com.terminal_devilal.core_processes.sync_data.service.DataSync;

@RestController
@RequestMapping("/pdv")
public class DeliveryPriceVolumeController {
	private DataSync dataSync;

	public DeliveryPriceVolumeController(DataSync dataSync) {
		this.dataSync = dataSync;
	}

	@GetMapping("/revise-data")
	public DataSyncProcessResponse processPdvDataTillDate() {
		return this.dataSync.processPdvDataTillDate();
	}
}