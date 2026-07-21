package com.terminal_devilal.core_processes.pipeline.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class PipelineAuditServiceTest {

    @Test
    void serializesAuditEventWithInstantTimestamp() throws JsonProcessingException {
        PipelineAuditService service = new PipelineAuditService();
        PipelineAuditEvent event = new PipelineAuditEvent(
                "SYNC-TEST",
                "ADOR",
                PipelineAuditStage.PDVT_SAVE,
                "SUCCESS",
                62,
                "2021-01-01",
                "2021-03-31",
                42L,
                "pool-1-thread-1",
                Instant.parse("2026-07-20T11:11:23Z"),
                "payload logged",
                null,
                Map.of("records", 62));

        String json = service.serializeEvent(event);

        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains("2026-07-20T11:11:23Z") || json.contains("2026-07-20T11:11:23.000Z"));
    }
}
