package com.terminal_devilal.business_tools.mannkendall.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallResponse;
import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallHistoryGenerateRequest;
import com.terminal_devilal.business_tools.mannkendall.dto.MkConfigRequest;
import com.terminal_devilal.business_tools.mannkendall.dto.MkConfigResponse;
import com.terminal_devilal.business_tools.mannkendall.dto.MkGenerationHistoryStatusUpdateRequest;
import com.terminal_devilal.business_tools.mannkendall.entity.MkGenerationHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.service.AnalyzeMannKendallForTicker;
import com.terminal_devilal.business_tools.mannkendall.service.MannKendallHistoryService;
import com.terminal_devilal.business_tools.mannkendall.service.MkConfigService;

@RestController
@RequestMapping("/api/devilal/mann-kendall")
public class MannKendallTrendAnalysisController {

	private final AnalyzeMannKendallForTicker analyzeMannKendallForTicker;
	private final MannKendallHistoryService mannKendallHistoryService;
	private final MkConfigService mkConfigService;

	@Autowired
	public MannKendallTrendAnalysisController(AnalyzeMannKendallForTicker analyzeMannKendallForTicker,
			MannKendallHistoryService mannKendallHistoryService, MkConfigService mkConfigService) {
		this.analyzeMannKendallForTicker = analyzeMannKendallForTicker;
		this.mannKendallHistoryService = mannKendallHistoryService;
		this.mkConfigService = mkConfigService;
	}

	@GetMapping("/log-close/trend")
	public ResponseEntity<List<MannKendallResponse>> getMannKendallAnalysis(
			@RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

		List<MannKendallResponse> results = analyzeMannKendallForTicker.getMannKendallTrendAnalysis(fromDate,
				toDate);
		return ResponseEntity.ok(results);
	}

	@PostMapping("/history/generate")
	public ResponseEntity<List<MkResultHistoryEntity>> generateHistory(
			@RequestBody MannKendallHistoryGenerateRequest request) {
		List<MkResultHistoryEntity> results = mannKendallHistoryService.generateHistory(request.getDate());
		return ResponseEntity.status(HttpStatus.CREATED).body(results);
	}

	@GetMapping("/history")
	public ResponseEntity<List<MkResultHistoryEntity>> getHistory(
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) Integer days, @RequestParam(required = false) List<String> tickers) {
		return ResponseEntity.ok(mannKendallHistoryService.getHistory(date, days, tickers));
	}

	@GetMapping("/generation-history")
	public ResponseEntity<List<MkGenerationHistoryEntity>> getGenerationHistory(
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ResponseEntity.ok(mannKendallHistoryService.getGenerationHistory(date));
	}

	@PutMapping("/generation-history/status")
	public ResponseEntity<MkGenerationHistoryEntity> updateGenerationHistoryStatus(
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam("days") Integer days, @RequestBody MkGenerationHistoryStatusUpdateRequest request) {
		return ResponseEntity.ok(mannKendallHistoryService.updateGenerationStatus(date, days, request.getStatus()));
	}

	@PostMapping("/config")
	public ResponseEntity<MkConfigResponse> createConfig(@RequestBody MkConfigRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(mkConfigService.create(request));
	}

	@PutMapping("/config/{days}")
	public ResponseEntity<MkConfigResponse> updateConfig(@PathVariable Integer days,
			@RequestBody MkConfigRequest request) {
		return ResponseEntity.ok(mkConfigService.update(days, request));
	}

	@GetMapping("/config")
	public ResponseEntity<List<MkConfigResponse>> listConfig() {
		return ResponseEntity.ok(mkConfigService.list());
	}

	@GetMapping("/config/{days}")
	public ResponseEntity<MkConfigResponse> getConfig(@PathVariable Integer days) {
		return ResponseEntity.ok(mkConfigService.get(days));
	}

	@PatchMapping("/config/{days}/enabled")
	public ResponseEntity<MkConfigResponse> setConfigEnabled(@PathVariable Integer days,
			@RequestParam Boolean enabled) {
		return ResponseEntity.ok(mkConfigService.setEnabled(days, enabled));
	}

	@DeleteMapping("/config/{days}")
	public ResponseEntity<Void> deleteConfig(@PathVariable Integer days) {
		mkConfigService.delete(days);
		return ResponseEntity.noContent().build();
	}

}
