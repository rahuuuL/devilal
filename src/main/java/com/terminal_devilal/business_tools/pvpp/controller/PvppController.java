package com.terminal_devilal.business_tools.pvpp.controller;

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

import com.terminal_devilal.business_tools.pvpp.dto.PvppConfigRequest;
import com.terminal_devilal.business_tools.pvpp.dto.PvppConfigResponse;
import com.terminal_devilal.business_tools.pvpp.dto.PvppGenerationHistoryStatusUpdateRequest;
import com.terminal_devilal.business_tools.pvpp.dto.PvppHistoryGenerateRequest;
import com.terminal_devilal.business_tools.pvpp.dto.PvppResultHistoryResponse;
import com.terminal_devilal.business_tools.pvpp.entity.PvppGenerationHistoryEntity;
import com.terminal_devilal.business_tools.pvpp.entity.PvppGenerationStatus;
import com.terminal_devilal.business_tools.pvpp.service.PvppConfigService;
import com.terminal_devilal.business_tools.pvpp.service.PvppHistoryService;

@RestController
@RequestMapping("/api/devilal/pvpp")
public class PvppController {

    private final PvppHistoryService pvppHistoryService;
    private final PvppConfigService pvppConfigService;

    @Autowired
    public PvppController(PvppHistoryService pvppHistoryService,
            PvppConfigService pvppConfigService) {
        this.pvppHistoryService = pvppHistoryService;
        this.pvppConfigService = pvppConfigService;
    }

    @PostMapping("/history/generate")
    public ResponseEntity<List<PvppResultHistoryResponse>> generateHistory(
            @RequestBody PvppHistoryGenerateRequest request) {
        List<PvppResultHistoryResponse> results = pvppHistoryService.generateHistory(request.getDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    @GetMapping("/history")
    public ResponseEntity<List<PvppResultHistoryResponse>> getHistory(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam("days") Integer days,
            @RequestParam(required = false) List<String> tickers) {
        return ResponseEntity.ok(pvppHistoryService.getHistory(fromDate, toDate, days, tickers));
    }

    @GetMapping("/generation-history")
    public ResponseEntity<List<PvppGenerationHistoryEntity>> getGenerationHistory(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(pvppHistoryService.getGenerationHistory(date));
    }

    @PutMapping("/generation-history/status")
    public ResponseEntity<PvppGenerationHistoryEntity> updateGenerationHistoryStatus(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("days") Integer days,
            @RequestBody PvppGenerationHistoryStatusUpdateRequest request) {
        return ResponseEntity.ok(pvppHistoryService.updateGenerationStatus(date, days, request.getStatus()));
    }

    @PostMapping("/config")
    public ResponseEntity<PvppConfigResponse> createConfig(@RequestBody PvppConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pvppConfigService.create(request));
    }

    @PutMapping("/config/{days}")
    public ResponseEntity<PvppConfigResponse> updateConfig(@PathVariable Integer days,
            @RequestBody PvppConfigRequest request) {
        return ResponseEntity.ok(pvppConfigService.update(days, request));
    }

    @GetMapping("/config")
    public ResponseEntity<List<PvppConfigResponse>> listConfig() {
        return ResponseEntity.ok(pvppConfigService.list());
    }

    @GetMapping("/config/{days}")
    public ResponseEntity<PvppConfigResponse> getConfig(@PathVariable Integer days) {
        return ResponseEntity.ok(pvppConfigService.get(days));
    }

    @PatchMapping("/config/{days}/enabled")
    public ResponseEntity<PvppConfigResponse> setConfigEnabled(@PathVariable Integer days,
            @RequestParam Boolean enabled) {
        return ResponseEntity.ok(pvppConfigService.setEnabled(days, enabled));
    }

    @DeleteMapping("/config/{days}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Integer days) {
        pvppConfigService.delete(days);
        return ResponseEntity.noContent().build();
    }
}
