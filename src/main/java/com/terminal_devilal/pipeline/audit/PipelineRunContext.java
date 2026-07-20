package com.terminal_devilal.pipeline.audit;

import java.time.Instant;
import java.util.UUID;

public class PipelineRunContext {
    private final String runId;
    private final Instant startedAt;

    public PipelineRunContext(String runId, Instant startedAt) {
        this.runId = runId;
        this.startedAt = startedAt;
    }

    public static PipelineRunContext create() {
        return new PipelineRunContext("SYNC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), Instant.now());
    }

    public String getRunId() {
        return runId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}
