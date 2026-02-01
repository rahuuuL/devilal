package com.terminal_devilal.indicators.volume.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.indicators.volume.model.ConsistentVolumeSignalResponse;
import com.terminal_devilal.indicators.volume.service.ConsistentVolumeDetector;

@RestController
@RequestMapping("/api/devilal/volume-analysis")
public class VolumeController {

	private final ConsistentVolumeDetector consistentVolumeDetector; // Service that has detectConsistantVolumes()

	public VolumeController(ConsistentVolumeDetector consistentVolumeDetector) {
		super();
		this.consistentVolumeDetector = consistentVolumeDetector;
	}

	@GetMapping("/consistent-signals/")
	public ResponseEntity<List<ConsistentVolumeSignalResponse>> getConsistentVolumeSignals2(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(defaultValue = "30") int baselineWindow,
			@RequestParam(defaultValue = "0.2") double baselineLowPercentile,
			@RequestParam(defaultValue = "0.8") double baselineHighPercentile,
			@RequestParam(defaultValue = "100") int rvolPercentileWindow,
			@RequestParam(defaultValue = "0.9") double rvolThresholdPercentile,
			@RequestParam(defaultValue = "10") int consistencyWindow,
			@RequestParam(defaultValue = "6") int requiredScore) throws Exception {

		List<ConsistentVolumeSignalResponse> signals = consistentVolumeDetector.detectConsistentVolumes(fromDate,
				toDate, baselineWindow, baselineLowPercentile, baselineHighPercentile, rvolPercentileWindow,
				rvolThresholdPercentile, consistencyWindow, requiredScore);

		return ResponseEntity.ok(signals);
	}
}