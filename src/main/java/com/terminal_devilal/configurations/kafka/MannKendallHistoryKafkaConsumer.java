package com.terminal_devilal.configurations.kafka;

import java.time.LocalDate;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terminal_devilal.business_tools.mannkendall.service.MannKendallHistoryService;

@Service
public class MannKendallHistoryKafkaConsumer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MannKendallHistoryService mannKendallHistoryService;

    public MannKendallHistoryKafkaConsumer(MannKendallHistoryService mannKendallHistoryService) {
        this.mannKendallHistoryService = mannKendallHistoryService;
    }

    @KafkaListener(topics = "mann-kendall-history", groupId = "devilal-group", containerFactory = "batchFactory", concurrency = "8")
    public void listen(List<ConsumerRecord<String, String>> records) {
        for (ConsumerRecord<String, String> record : records) {
            try {
                JsonNode payload = MAPPER.readTree(record.value());
                JsonNode dateNode = payload.get("date");
                if (dateNode == null || dateNode.isNull()) {
                    continue;
                }
                LocalDate processingDate = LocalDate.parse(dateNode.asText());
                mannKendallHistoryService.generateHistory(processingDate);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Unable to parse Mann-Kendall history event", e);
            }
        }
    }
}
