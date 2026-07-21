package com.terminal_devilal.utils.resilientbatchservice;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.utils.deadletterqueue.DeadLetterQueue;

import jakarta.transaction.Transactional;

public abstract class ResilientBatchService<T> {

    private static final Logger log = LoggerFactory.getLogger(ResilientBatchService.class);

    private final ConcurrentLinkedQueue<T> buffer = new ConcurrentLinkedQueue<>();
    protected final DeadLetterQueue<T> dlq = new DeadLetterQueue<>();
    protected final PipelineAuditService pipelineAuditService;

    protected ResilientBatchService(PipelineAuditService pipelineAuditService) {
        this.pipelineAuditService = pipelineAuditService;
    }

    protected void enqueue(T record) {
        buffer.add(record);
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void flushBuffer() {
        List<T> batch = new ArrayList<>();
        T item;
        while ((item = buffer.poll()) != null) {
            batch.add(item);
        }

        if (batch.isEmpty()) {
            return;
        }

        try {
            saveAll(batch);
        } catch (Exception batchEx) {
            log.error("Batch save failed for {} records", batch.size(), batchEx);
            pipelineAuditService.logEvent(null, resolveTicker(batch.get(0)), PipelineAuditStage.BATCH_FLUSH,
                    "FAILURE", batch.size(), null, null, null, "batch save failed", batchEx.getMessage(),
                    buildDetails(batch.size(), batchEx, batch.get(0)));

            for (T record : batch) {
                try {
                    saveOne(record);
                } catch (Exception recordEx) {
                    if (isDuplicateKey(recordEx)) {
                        log.info("Duplicate record skipped during saveOne fallback: {}", record);
                        pipelineAuditService.logEvent(null, resolveTicker(record), PipelineAuditStage.BATCH_FLUSH,
                                "SKIP", 1, null, null, null, "duplicate record skipped", recordEx.getMessage(),
                                buildDetails(1, recordEx, record));
                        continue;
                    }
                    log.error("Single-record save failed for {}", record, recordEx);
                    pipelineAuditService.logEvent(null, resolveTicker(record), PipelineAuditStage.BATCH_FLUSH,
                            "FAILURE", 1, null, null, null, "single-record save failed", recordEx.getMessage(),
                            buildDetails(1, recordEx, record));
                    dlq.add(record, recordEx);
                }
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void retryDeadLetters() {
        List<DeadLetterQueue.FailedRecord<T>> failed = dlq.drainAll();
        if (failed.isEmpty()) {
            return;
        }

        log.warn("Retrying {} failed record(s)", failed.size());

        for (DeadLetterQueue.FailedRecord<T> entry : failed) {
            try {
                saveOne(entry.record());
                log.info("Retry succeeded for record originally failed at {}", entry.failedAt());
            } catch (Exception retryEx) {
                log.error("Permanent failure for record {}", entry.record(), retryEx);
                pipelineAuditService.logEvent(null, resolveTicker(entry.record()), PipelineAuditStage.BATCH_FLUSH,
                        "FAILURE", 1, null, null, null, "dead-letter retry failed", retryEx.getMessage(),
                        buildDetails(1, retryEx, entry.record()));
                onPermanentFailure(entry.record(), retryEx);
            }
        }
    }

    protected void onPermanentFailure(T record, Exception e) {
    }

    protected abstract void saveAll(List<T> batch);

    protected abstract void saveOne(T record);

    private Map<String, Object> buildDetails(int batchSize, Exception error, T record) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchSize", batchSize);
        details.put("entity", record);
        details.put("timestamp", Instant.now().toString());
        details.put("stacktrace", error.getStackTrace());
        return details;
    }

    private String resolveTicker(T record) {
        if (record == null) {
            return null;
        }
        try {
            Method method = record.getClass().getMethod("getTicker");
            Object value = method.invoke(record);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isDuplicateKey(Exception error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("Duplicate entry")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
