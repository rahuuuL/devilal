package com.terminal_devilal.pipeline.audit;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class PipelineAuditService {
    private static final Logger auditLogger = LoggerFactory.getLogger("PIPELINE_AUDIT");
    private static final Logger log = LoggerFactory.getLogger(PipelineAuditService.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ConcurrentHashMap<String, PipelineTickerContext> tickerContexts = new ConcurrentHashMap<>();

    @Value("${pipeline.audit.enabled:true}")
    private boolean enabled;

    @Value("${pipeline.audit.log.payload:false}")
    private boolean logPayload;

    public PipelineRunContext startRun() {
        return PipelineRunContext.create();
    }

    public PipelineTickerContext startTickerContext(String runId, String ticker) {
        PipelineTickerContext context = new PipelineTickerContext(ticker, runId);
        tickerContexts.put(buildTickerKey(runId, ticker), context);
        return context;
    }

    public void logEvent(PipelineTickerContext context, PipelineAuditStage stage, String status, Integer records,
                         String firstDate, String lastDate, Long durationMs, String message, String error,
                         Map<String, Object> details) {
        if (!enabled) {
            return;
        }
        logEvent(context.getRunId(), context.getTicker(), stage, status, records, firstDate, lastDate, durationMs,
                message, error, details);
    }

    public void logEvent(String runId, String ticker, PipelineAuditStage stage, String status, Integer records,
                         String firstDate, String lastDate, Long durationMs, String message, String error,
                         Map<String, Object> details) {
        if (!enabled) {
            return;
        }
        PipelineAuditEvent event = new PipelineAuditEvent(
                runId,
                ticker,
                stage,
                status,
                records,
                firstDate,
                lastDate,
                durationMs,
                Thread.currentThread().getName(),
                Instant.now(),
                message,
                error,
                details);
        try {
            auditLogger.info(serializeEvent(event));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize pipeline audit event", e);
        }
    }

    String serializeEvent(PipelineAuditEvent event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event);
    }

    public void logPayload(String ticker, String payload, PipelineAuditStage stage) {
        if (!enabled || !logPayload) {
            return;
        }
        Map<String, Object> details = new HashMap<>();
        details.put("payload", payload);
        logEvent("DEFAULT", ticker, stage, "SUCCESS", null, null, null, null, "payload logged", null, details);
    }

    public void logStageStart(PipelineTickerContext context, PipelineAuditStage stage, String message) {
        logEvent(context, stage, "START", null, null, null, null, message, null, null);
    }

    public void logStageSuccess(PipelineTickerContext context, PipelineAuditStage stage, Integer records,
                                String firstDate, String lastDate, Long durationMs, String message) {
        logEvent(context, stage, "SUCCESS", records, firstDate, lastDate, durationMs, message, null, null);
    }

    public void logStageFailure(PipelineTickerContext context, PipelineAuditStage stage, String message, Exception error) {
        if (context != null) {
            context.incrementError();
        }
        logEvent(context, stage, "FAILURE", null, null, null, null, message, error == null ? null : error.getMessage(), null);
    }

    public void logTickerSummary(PipelineTickerContext context, long durationMs) {
        if (!enabled || context == null) {
            return;
        }
        PipelineMetrics metrics = context.getMetrics();
        Map<String, Object> details = new HashMap<>();
        details.put("apiReceived", metrics.getApiReceived());
        details.put("parsed", metrics.getParsed());
        details.put("pdvtSaved", metrics.getPdvtSaved());
        details.put("kafkaPublished", metrics.getKafkaPublished());
        details.put("kafkaConsumed", metrics.getKafkaConsumed());
        details.put("atrProcessed", metrics.getAtrProcessed());
        details.put("atrSaved", metrics.getAtrSaved());
        details.put("rsiProcessed", metrics.getRsiProcessed());
        details.put("rsiSaved", metrics.getRsiSaved());
        details.put("vwapProcessed", metrics.getVwapProcessed());
        details.put("vwapSaved", metrics.getVwapSaved());
        details.put("errors", context.getErrorCount());
        details.put("durationMs", durationMs);
        logEvent(context, PipelineAuditStage.TICKER_SUMMARY, "SUCCESS", null, null, null, durationMs,
                "ticker summary", null, details);
    }

    public PipelineTickerContext getOrCreateContext(String ticker) {
        return tickerContexts.computeIfAbsent("default:" + ticker, ignored -> new PipelineTickerContext(ticker, "DEFAULT"));
    }

    private String buildTickerKey(String runId, String ticker) {
        return runId + ":" + ticker;
    }
}
