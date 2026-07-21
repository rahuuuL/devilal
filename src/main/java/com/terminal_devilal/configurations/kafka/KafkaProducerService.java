package com.terminal_devilal.configurations.kafka;

import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineTickerContext;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PipelineAuditService pipelineAuditService;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, PipelineAuditService pipelineAuditService) {
        this.kafkaTemplate = kafkaTemplate;
        this.pipelineAuditService = pipelineAuditService;
    }

    public void sendMessage(String topic, String key, String message, PipelineTickerContext tickerContext) {
        long startedAt = System.currentTimeMillis();
        try {
            SendResult<String, String> result = kafkaTemplate.send(topic, key, message).get();
            RecordMetadata metadata = result.getRecordMetadata();
            if (tickerContext != null) {
                tickerContext.getMetrics().incrementKafkaPublished();
                pipelineAuditService.logEvent(tickerContext.getRunId(), tickerContext.getTicker(), PipelineAuditStage.KAFKA_PUBLISH,
                        "SUCCESS", 1, null, null, System.currentTimeMillis() - startedAt,
                        "Kafka publish succeeded", null, null);
            }
            log.info("Kafka publish succeeded topic={} partition={} offset={} key={} durationMs={}", metadata.topic(), metadata.partition(), metadata.offset(), key, System.currentTimeMillis() - startedAt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Kafka publish interrupted topic={} key={}", topic, key, e);
            if (tickerContext != null) {
                pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.KAFKA_PUBLISH,
                        "Kafka publish interrupted", e);
            }
        } catch (ExecutionException e) {
            log.error("Kafka publish failed topic={} key={}", topic, key, e);
            if (tickerContext != null) {
                pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.KAFKA_PUBLISH,
                        "Kafka publish failed", e);
            }
        }
    }
}
