package com.terminal_devilal.core_processes.pipeline.audit;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class PipelineTickerContext {
    private final String ticker;
    private final String runId;
    private final Instant startedAt;
    private final PipelineMetrics metrics = new PipelineMetrics();
    private final AtomicInteger errors = new AtomicInteger();

    public PipelineTickerContext(String ticker, String runId) {
        this.ticker = ticker;
        this.runId = runId;
        this.startedAt = Instant.now();
    }

    public String getTicker() {
        return ticker;
    }

    public String getRunId() {
        return runId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public PipelineMetrics getMetrics() {
        return metrics;
    }

    public void incrementError() {
        errors.incrementAndGet();
    }

    public int getErrorCount() {
        return errors.get();
    }
}
