package com.terminal_devilal.configurations.kakfa;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terminal_devilal.indicators.atr.service.AverageTrueRangeService;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeUtility;
import com.terminal_devilal.indicators.rsi.service.RSIService;
import com.terminal_devilal.indicators.vwap.service.VWAPService;
import com.terminal_devilal.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.pipeline.audit.PipelineTickerContext;

@Service
public class KafkaMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int FLUSH_EVERY = 500;

    private final AverageTrueRangeService averageTrueRangeService;
    private final RSIService rsiService;
    private final VWAPService vwapService;
    private final PriceDeliveryVolumeUtility priceDeliveryVolumeUtility;
    private final PipelineAuditService pipelineAuditService;

    public KafkaMessageConsumer(AverageTrueRangeService averageTrueRangeService, RSIService rsiService,
            VWAPService vwapService, PriceDeliveryVolumeUtility priceDeliveryVolumeUtility,
            PipelineAuditService pipelineAuditService) {
        this.averageTrueRangeService = averageTrueRangeService;
        this.rsiService = rsiService;
        this.vwapService = vwapService;
        this.priceDeliveryVolumeUtility = priceDeliveryVolumeUtility;
        this.pipelineAuditService = pipelineAuditService;
    }

    @KafkaListener(topics = "pdv-data", groupId = "devilal-group", concurrency = "8", containerFactory = "batchFactory")
    public void listen(List<ConsumerRecord<String, String>> records) {
        Instant startedAt = Instant.now();
        AtomicInteger processedCount = new AtomicInteger();
        String firstTicker = null;
        String lastTicker = null;

        for (ConsumerRecord<String, String> record : records) {
            Optional<PriceDeliveryVolumeEntity> optionalEntity = parseEntity(record.key(), record.value());
            if (optionalEntity.isEmpty()) {
                continue;
            }

            PriceDeliveryVolumeEntity entity = optionalEntity.get();
            if (firstTicker == null) {
                firstTicker = entity.getTicker();
            }
            lastTicker = entity.getTicker();

            PipelineTickerContext tickerContext = pipelineAuditService.startTickerContext("KAFKA", entity.getTicker());
            tickerContext.getMetrics().incrementKafkaConsumed();
            pipelineAuditService.logStageStart(tickerContext, PipelineAuditStage.KAFKA_CONSUME, "Kafka batch processing started");

            try {
                averageTrueRangeService.processATR(entity, tickerContext);
            } catch (Exception e) {
                pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.ATR_PROCESS, "ATR processing failed", e);
            }

            try {
                rsiService.processRSI(entity, tickerContext);
            } catch (Exception e) {
                pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.RSI_PROCESS, "RSI processing failed", e);
            }

            try {
                vwapService.processVwap(entity, tickerContext);
            } catch (Exception e) {
                pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.VWAP_PROCESS, "VWAP processing failed", e);
            }

            processedCount.incrementAndGet();
            if (processedCount.get() % FLUSH_EVERY == 0) {
                averageTrueRangeService.flushBuffer();
                rsiService.flushBuffer();
                vwapService.flushBuffer();
            }
        }

        averageTrueRangeService.flushBuffer();
        rsiService.flushBuffer();
        vwapService.flushBuffer();

        pipelineAuditService.logEvent("KAFKA", firstTicker == null ? "UNKNOWN" : firstTicker,
                PipelineAuditStage.KAFKA_CONSUME, "SUCCESS", records.size(), firstTicker, lastTicker,
                Duration.between(startedAt, Instant.now()).toMillis(), "Kafka batch consumed", null, null);
    }

    private Optional<PriceDeliveryVolumeEntity> parseEntity(String ticker, String data) {
        try {
            JsonNode node = mapper.readTree(data);
            PriceDeliveryVolumeEntity entity = priceDeliveryVolumeUtility.parseStockData(node, ticker);
            return Optional.of(entity);
        } catch (JsonProcessingException e) {
            log.error("Kafka consumer JSON parse error", e);
            return Optional.empty();
        }
    }
}
