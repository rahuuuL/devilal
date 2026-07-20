package com.terminal_devilal.pipeline.audit;

import java.time.Instant;
import java.util.Map;

public record PipelineAuditEvent(
        String runId,
        String ticker,
        PipelineAuditStage stage,
        String status,
        Integer records,
        String firstDate,
        String lastDate,
        Long durationMs,
        String thread,
        Instant timestamp,
        String message,
        String error,
        Map<String, Object> details) {
}
