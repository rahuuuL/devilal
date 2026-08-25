package com.terminal_devilal.business_tools.pvpp.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.business_tools.pvpp.calculator.PvppCalcResult;
import com.terminal_devilal.business_tools.pvpp.calculator.PvppCalculator;
import com.terminal_devilal.business_tools.pvpp.dto.PvppResultHistoryResponse;
import com.terminal_devilal.business_tools.pvpp.entity.PvppConfigEntity;
import com.terminal_devilal.business_tools.pvpp.entity.PvppGenerationHistoryEntity;
import com.terminal_devilal.business_tools.pvpp.entity.PvppGenerationHistoryId;
import com.terminal_devilal.business_tools.pvpp.entity.PvppGenerationStatus;
import com.terminal_devilal.business_tools.pvpp.entity.PvppResultHistoryEntity;
import com.terminal_devilal.business_tools.pvpp.repository.PvppConfigRepository;
import com.terminal_devilal.business_tools.pvpp.repository.PvppGenerationHistoryRepository;
import com.terminal_devilal.business_tools.pvpp.repository.PvppResultHistoryCustomRepository;
import com.terminal_devilal.business_tools.pvpp.repository.PvppResultHistoryRepository;
import com.terminal_devilal.indicators.pdv.cache.PDVCacheService;
import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;

import io.micrometer.core.annotation.Timed;

@Service
public class PvppHistoryService {

    private static final Logger log = LoggerFactory.getLogger(PvppHistoryService.class);

    private final PvppConfigRepository pvppConfigRepository;
    private final PvppGenerationHistoryRepository pvppGenerationHistoryRepository;
    private final PvppResultHistoryRepository pvppResultHistoryRepository;
    private final PvppResultHistoryCustomRepository pvppResultHistoryCustomRepository;
    private final PvppCalculator pvppCalculator;
    private final PDVCacheService pdvCacheService;

    public PvppHistoryService(PvppConfigRepository pvppConfigRepository,
            PvppGenerationHistoryRepository pvppGenerationHistoryRepository,
            PvppResultHistoryRepository pvppResultHistoryRepository,
            PvppResultHistoryCustomRepository pvppResultHistoryCustomRepository,
            PvppCalculator pvppCalculator,
            PDVCacheService pdvCacheService) {
        this.pvppConfigRepository = pvppConfigRepository;
        this.pvppGenerationHistoryRepository = pvppGenerationHistoryRepository;
        this.pvppResultHistoryRepository = pvppResultHistoryRepository;
        this.pvppResultHistoryCustomRepository = pvppResultHistoryCustomRepository;
        this.pvppCalculator = pvppCalculator;
        this.pdvCacheService = pdvCacheService;
    }

    @Transactional
    @Timed(value = "pvpp.history.generate", description = "PVPP history generation")
    public List<PvppResultHistoryResponse> generateHistory(LocalDate processingDate) {
        if (processingDate == null) {
            throw new IllegalArgumentException("processingDate must not be null");
        }

        long tStart = System.nanoTime();
        long tStep = tStart;

        List<PvppConfigEntity> configs = pvppConfigRepository.findByEnabledTrueOrderByDaysAsc();
        List<Integer> enabledDays = configs.stream()
                .map(PvppConfigEntity::getDays)
                .filter(Objects::nonNull)
                .filter(days -> days > 0)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (enabledDays.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> successfulDays = pvppGenerationHistoryRepository.findByDateAndStatus(processingDate, PvppGenerationStatus.SUCESS)
                .stream()
                .map(PvppGenerationHistoryEntity::getDays)
                .collect(Collectors.toSet());

        List<Integer> daysToProcess = enabledDays.stream()
                .filter(days -> !successfulDays.contains(days))
                .collect(Collectors.toList());

        if (daysToProcess.isEmpty()) {
            return Collections.emptyList();
        }

        tStep = logElapsed("config resolution", processingDate, tStep);

        int maxLookback = daysToProcess.stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> tickers = pdvCacheService.findDistinctTicker();

        tStep = logElapsed("ticker list fetch (" + tickers.size() + " tickers)", processingDate, tStep);

        for (Integer days : daysToProcess) {
            upsertGenerationHistory(processingDate, days, PvppGenerationStatus.IN_PROGRESS, null, null);
        }

        tStep = logElapsed("generation history IN_PROGRESS upsert", processingDate, tStep);

        // -------- Fetch per-ticker series in parallel --------
        Map<String, List<PriceDeliveryVolumeEntity>> tickerSeries = new ConcurrentHashMap<>();
        Map<Integer, Set<String>> skippedTickersByDay = new ConcurrentHashMap<>();

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<CompletableFuture<Void>> fetchFutures = new ArrayList<>();
            for (String ticker : tickers) {
                fetchFutures.add(CompletableFuture.runAsync(() -> {
                    try {
                        // Bound the upper end at processingDate - without it the cache returns every date up to
                        // "now", which blew up row counts (and could leak future data into the rolling windows).
                        List<PriceDeliveryVolumeEntity> series = pdvCacheService
                                .findByTickerAndDateBetweenOrderByDateAsc(ticker,
                                        processingDate.minusDays(maxLookback), processingDate);
                        if (series != null && !series.isEmpty()) {
                            tickerSeries.put(ticker, series);
                        }
                    } catch (Exception e) {
                        log.warn("PVPP generation failed for ticker {} on {}", ticker, processingDate, e);
                        markSkipped(skippedTickersByDay, daysToProcess, ticker);
                    }
                }, executor));
            }
            CompletableFuture.allOf(fetchFutures.toArray(new CompletableFuture[0])).join();

            tStep = logElapsed("series fetch for " + tickerSeries.size() + " tickers", processingDate, tStep);

            // -------- Compute PVPP windows per ticker in parallel --------
            Queue<PvppResultHistoryEntity> historyQueue = new ConcurrentLinkedQueue<>();

            List<CompletableFuture<Void>> computeFutures = new ArrayList<>();
            for (Map.Entry<String, List<PriceDeliveryVolumeEntity>> entry : tickerSeries.entrySet()) {
                String ticker = entry.getKey();
                List<PriceDeliveryVolumeEntity> series = entry.getValue();
                computeFutures.add(CompletableFuture.runAsync(() -> {
                    try {
                        // Only emit rows for processingDate; earlier dates in the lookback window were already
                        // generated on their own run and re-emitting them each day was the main cause of bloat.
                        PvppCalcResult calcResult = pvppCalculator.computeForDate(ticker, series, daysToProcess,
                                processingDate);
                        if (calcResult.hasHistoryRows()) {
                            historyQueue.addAll(calcResult.getHistoryRows());
                        }
                        if (!calcResult.getSkippedTickers().isEmpty()) {
                            for (Integer days : daysToProcess) {
                                skippedTickersByDay.computeIfAbsent(days, k -> ConcurrentHashMap.newKeySet())
                                        .addAll(calcResult.getSkippedTickers());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("PVPP generation failed for ticker {} on {}", ticker, processingDate, e);
                        markSkipped(skippedTickersByDay, daysToProcess, ticker);
                    }
                }, executor));
            }
            CompletableFuture.allOf(computeFutures.toArray(new CompletableFuture[0])).join();

            List<PvppResultHistoryEntity> historyRows = new ArrayList<>(historyQueue);
            Map<Integer, String> skippedByDay = new HashMap<>();
            skippedTickersByDay.forEach((days, set) -> skippedByDay.put(days, String.join(",", set)));

            tStep = logElapsed("PVPP window computation for " + tickerSeries.size() + " tickers", processingDate, tStep);

            if (!historyRows.isEmpty()) {
                pvppResultHistoryCustomRepository.upsertHistoryBatch(historyRows);
            }
            tStep = logElapsed("history rows batch upsert (" + historyRows.size() + " rows)", processingDate, tStep);

            for (Integer days : daysToProcess) {
                try {
                    boolean hasRowsForDay = historyRows.stream()
                            .anyMatch(row -> row.getDays() != null && row.getDays().equals(days));
                    if (!hasRowsForDay && skippedByDay.get(days) == null) {
                        upsertGenerationHistory(processingDate, days, PvppGenerationStatus.SUCESS, null, null);
                        continue;
                    }

                    upsertGenerationHistory(processingDate, days, PvppGenerationStatus.SUCESS, null, skippedByDay.get(days));
                } catch (Exception e) {
                    log.warn("PVPP generation finalization failed for days {}", days, e);
                    upsertGenerationHistory(processingDate, days, PvppGenerationStatus.FAILED, safeErrorMessage(e), skippedByDay.get(days));
                }
            }

            logElapsed("generation history finalization", processingDate, tStep);

            List<PvppResultHistoryResponse> responses = new ArrayList<>();
            for (PvppResultHistoryEntity entity : historyRows) {
                responses.add(toResponse(entity));
            }

            log.info("PVPP[{}] history generation completed in {} ms total", processingDate,
                    (System.nanoTime() - tStart) / 1_000_000);

            return responses;
        } finally {
            executor.shutdown();
        }
    }

    private void markSkipped(Map<Integer, Set<String>> skippedTickersByDay, List<Integer> daysToProcess, String ticker) {
        for (Integer days : daysToProcess) {
            skippedTickersByDay.computeIfAbsent(days, k -> ConcurrentHashMap.newKeySet()).add(ticker);
        }
    }

    private long logElapsed(String stepName, LocalDate processingDate, long previousStepNanos) {
        long now = System.nanoTime();
        log.info("PVPP[{}] {} took {} ms", processingDate, stepName, (now - previousStepNanos) / 1_000_000);
        return now;
    }

    public List<PvppResultHistoryResponse> getHistory(LocalDate fromDate, LocalDate toDate, Integer days,
            List<String> tickers) {
        if (fromDate == null) {
            throw new IllegalArgumentException("fromDate must not be null");
        }
        if (toDate == null) {
            throw new IllegalArgumentException("toDate must not be null");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must not be before fromDate");
        }
        if (days == null || days <= 0) {
            throw new IllegalArgumentException("days must be positive");
        }

        Set<String> normalizedTickers = normalizeTickers(tickers);
        List<PvppResultHistoryEntity> entities;
        if (normalizedTickers.isEmpty()) {
            entities = pvppResultHistoryRepository.findByDateBetweenAndDaysOrderByDateDesc(fromDate, toDate, days);
        } else {
            entities = pvppResultHistoryRepository.findByDateBetweenAndDaysAndTickerInOrderByDateDesc(
                    fromDate, toDate, days, normalizedTickers);
        }

        List<PvppResultHistoryResponse> responses = new ArrayList<>();
        for (PvppResultHistoryEntity entity : entities) {
            responses.add(toResponse(entity));
        }
        return responses;
    }

    public List<PvppGenerationHistoryEntity> getGenerationHistory(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        return pvppGenerationHistoryRepository.findByDate(date);
    }

    @Transactional
    public PvppGenerationHistoryEntity updateGenerationStatus(LocalDate date, Integer days, PvppGenerationStatus status) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        if (days == null || days <= 0) {
            throw new IllegalArgumentException("days must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        PvppGenerationHistoryId id = new PvppGenerationHistoryId(date, days);
        PvppGenerationHistoryEntity entity = pvppGenerationHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Generation history not found for date=" + date + " days=" + days));

        entity.setStatus(status);
        if (status == PvppGenerationStatus.SUCESS || status == PvppGenerationStatus.FAILED) {
            entity.setErrorMessage(null);
        }
        return pvppGenerationHistoryRepository.save(entity);
    }

    private Set<String> normalizeTickers(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Set.of();
        }
        return tickers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    private void upsertGenerationHistory(LocalDate date, Integer days, PvppGenerationStatus status,
            String errorMessage, String skippedTickers) {
        PvppGenerationHistoryEntity history = new PvppGenerationHistoryEntity();
        history.setDate(date);
        history.setDays(days);
        history.setStatus(status);
        history.setErrorMessage(errorMessage);
        history.setSkippedTickers(skippedTickers);
        pvppGenerationHistoryRepository.save(history);
    }

    private PvppResultHistoryResponse toResponse(PvppResultHistoryEntity entity) {
        PvppResultHistoryResponse response = new PvppResultHistoryResponse();
        response.setTicker(entity.getTicker());
        response.setDate(entity.getDate());
        response.setDays(entity.getDays());
        response.setReturnPct(entity.getReturnPct());
        response.setVolume(Double.valueOf(entity.getVolume() != null ? entity.getVolume() : 0L));
        response.setClv(entity.getClv());
        response.setCenteredClv(entity.getCenteredClv());
        response.setRvol(entity.getRvol());
        response.setEfficiency(entity.getEfficiency());
        return response;
    }

    private String safeErrorMessage(Exception e) {
        if (e == null) {
            return "Unknown PVPP history generation error";
        }
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
