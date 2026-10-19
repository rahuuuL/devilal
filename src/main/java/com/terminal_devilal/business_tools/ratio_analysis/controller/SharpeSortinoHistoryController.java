package com.terminal_devilal.business_tools.ratio_analysis.controller;

import java.time.LocalDate;
import java.util.List;

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

import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoConfigRequest;
import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoConfigResponse;
import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoGenerationHistoryStatusUpdateRequest;
import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoHistoryGenerateRequest;
import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoHistoryResponse;
import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoGenerationHistoryEntity;
import com.terminal_devilal.business_tools.ratio_analysis.service.SharpeSortinoConfigService;
import com.terminal_devilal.business_tools.ratio_analysis.service.SharpeSortinoHistoryService;

@RestController
@RequestMapping("/api/devilal/ratios")
public class SharpeSortinoHistoryController {

    private final SharpeSortinoHistoryService historyService;
    private final SharpeSortinoConfigService configService;

    public SharpeSortinoHistoryController(SharpeSortinoHistoryService historyService,
            SharpeSortinoConfigService configService) {
        this.historyService = historyService;
        this.configService = configService;
    }

    @PostMapping("/history/generate")
    public ResponseEntity<List<SharpeSortinoHistoryResponse>> generateHistory(
            @RequestBody SharpeSortinoHistoryGenerateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(historyService.generateHistory(request.getDate()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SharpeSortinoHistoryResponse>> getHistory(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam("days") Integer days,
            @RequestParam(required = false) List<String> tickers) {
        return ResponseEntity.ok(historyService.getHistory(fromDate, toDate, days, tickers));
    }

    @GetMapping("/generation-history")
    public ResponseEntity<List<SharpeSortinoGenerationHistoryEntity>> getGenerationHistory(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(historyService.getGenerationHistory(date));
    }

    @PutMapping("/generation-history/status")
    public ResponseEntity<SharpeSortinoGenerationHistoryEntity> updateGenerationHistoryStatus(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("days") Integer days,
            @RequestBody SharpeSortinoGenerationHistoryStatusUpdateRequest request) {
        return ResponseEntity.ok(historyService.updateGenerationStatus(date, days, request.getStatus()));
    }

    @PostMapping("/config")
    public ResponseEntity<SharpeSortinoConfigResponse> createConfig(@RequestBody SharpeSortinoConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(configService.create(request));
    }

    @PutMapping("/config/{days}")
    public ResponseEntity<SharpeSortinoConfigResponse> updateConfig(@PathVariable Integer days,
            @RequestBody SharpeSortinoConfigRequest request) {
        return ResponseEntity.ok(configService.update(days, request));
    }

    @GetMapping("/config")
    public ResponseEntity<List<SharpeSortinoConfigResponse>> listConfig() {
        return ResponseEntity.ok(configService.list());
    }

    @GetMapping("/config/{days}")
    public ResponseEntity<SharpeSortinoConfigResponse> getConfig(@PathVariable Integer days) {
        return ResponseEntity.ok(configService.get(days));
    }

    @PatchMapping("/config/{days}/enabled")
    public ResponseEntity<SharpeSortinoConfigResponse> setConfigEnabled(@PathVariable Integer days,
            @RequestParam Boolean enabled) {
        return ResponseEntity.ok(configService.setEnabled(days, enabled));
    }

    @DeleteMapping("/config/{days}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Integer days) {
        configService.delete(days);
        return ResponseEntity.noContent().build();
    }
}
