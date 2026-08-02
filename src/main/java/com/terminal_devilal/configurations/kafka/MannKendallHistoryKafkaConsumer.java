package com.terminal_devilal.configurations.kafka;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terminal_devilal.business_tools.mannkendall.service.MannKendallHistoryService;

import jakarta.annotation.PreDestroy;

@Service
public class MannKendallHistoryKafkaConsumer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_DELAY_MS = 2000L;

    private final MannKendallHistoryService mannKendallHistoryService;
    private final ConcurrentLinkedQueue<LocalDate> pendingDates = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingCount = new AtomicInteger();
    private final AtomicBoolean flushing = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()));

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
                pendingDates.add(processingDate);
                pendingCount.incrementAndGet();
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Unable to parse Mann-Kendall history event", e);
            }
        }

        if (pendingCount.get() >= BATCH_SIZE) {
            flushPendingDates();
        }
    }

    @Scheduled(fixedDelay = FLUSH_DELAY_MS)
    public void flushByTime() {
        flushPendingDates();
    }

    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdownNow();
    }

    private void flushPendingDates() {
        if (!flushing.compareAndSet(false, true)) {
            return;
        }

        try {
            List<LocalDate> batch = drainPendingDates();
            if (batch.isEmpty()) {
                return;
            }

            Set<LocalDate> uniqueDates = new LinkedHashSet<>(batch);
            List<CompletableFuture<Void>> tasks = new ArrayList<>();

            for (LocalDate date : uniqueDates) {
                tasks.add(CompletableFuture.runAsync(() -> mannKendallHistoryService.generateHistory(date), executor)
                        .exceptionally(ex -> {
                            throw new IllegalStateException("Failed to generate Mann-Kendall history for date " + date,
                                    ex);
                        }));
            }

            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
        } finally {
            flushing.set(false);
            if (pendingCount.get() >= BATCH_SIZE) {
                flushPendingDates();
            }
        }
    }

    private List<LocalDate> drainPendingDates() {
        List<LocalDate> batch = new ArrayList<>();
        LocalDate date;
        while ((date = pendingDates.poll()) != null) {
            batch.add(date);
            pendingCount.decrementAndGet();
        }
        return batch;
    }
}
