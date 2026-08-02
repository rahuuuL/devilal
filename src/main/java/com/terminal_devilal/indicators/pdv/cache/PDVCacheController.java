package com.terminal_devilal.indicators.pdv.cache;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache/pdv")
public class PDVCacheController {

    private final PDVCacheService pdvCacheService;

    public PDVCacheController(PDVCacheService pdvCacheService) {
        this.pdvCacheService = pdvCacheService;
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, String>> reloadCache() {
        pdvCacheService.reloadCache();
        pdvCacheService.persistSnapshot();
        return ResponseEntity.ok(Map.of("status", "ok", "message", "PDV cache reloaded"));
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        pdvCacheService.clearCache();
        return ResponseEntity.ok(Map.of("status", "ok", "message", "PDV memory cache cleared"));
    }

    @PostMapping("/chronicle/clear")
    public ResponseEntity<Map<String, Object>> clearChronicle() {
        boolean deleted = pdvCacheService.clearChronicleSnapshot();
        return ResponseEntity.ok(Map.of("status", "ok", "deleted", deleted));
    }

    @PostMapping("/chronicle/reload")
    public ResponseEntity<Map<String, Object>> reloadChronicle() {
        boolean loaded = pdvCacheService.reloadFromChronicleSnapshot();
        return ResponseEntity.ok(Map.of("status", "ok", "loaded", loaded));
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return pdvCacheService.getCacheStats();
    }
}
