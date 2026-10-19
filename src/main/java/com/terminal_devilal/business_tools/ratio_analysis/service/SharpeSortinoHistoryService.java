package com.terminal_devilal.business_tools.ratio_analysis.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoHistoryResponse;
import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoConfigEntity;
import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoGenerationHistoryEntity;
import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoGenerationHistoryId;
import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoGenerationStatus;
import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoResultHistoryEntity;
import com.terminal_devilal.business_tools.ratio_analysis.repository.SharpeSortinoConfigRepository;
import com.terminal_devilal.business_tools.ratio_analysis.repository.SharpeSortinoGenerationHistoryRepository;
import com.terminal_devilal.business_tools.ratio_analysis.repository.SharpeSortinoResultHistoryRepository;
import com.terminal_devilal.indicators.pdv.cache.PDVCacheService;
import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;

import io.micrometer.core.annotation.Timed;

@Service
public class SharpeSortinoHistoryService {

    private static final Logger log = LoggerFactory.getLogger(SharpeSortinoHistoryService.class);
    private final SharpeSortinoConfigRepository configRepository;
    private final SharpeSortinoGenerationHistoryRepository generationHistoryRepository;
    private final SharpeSortinoResultHistoryRepository resultHistoryRepository;
    private final PDVCacheService pdvCacheService;

    public SharpeSortinoHistoryService(SharpeSortinoConfigRepository configRepository,
            SharpeSortinoGenerationHistoryRepository generationHistoryRepository,
            SharpeSortinoResultHistoryRepository resultHistoryRepository,
            PDVCacheService pdvCacheService) {
        this.configRepository = configRepository;
        this.generationHistoryRepository = generationHistoryRepository;
        this.resultHistoryRepository = resultHistoryRepository;
        this.pdvCacheService = pdvCacheService;
    }

    @Timed(value = "sharpe_sortino.history.generate", description = "Sharpe/Sortino history generation")
    public List<SharpeSortinoHistoryResponse> generateHistory(LocalDate processingDate) {
        if (processingDate == null) {
            throw new IllegalArgumentException("processingDate must not be null");
        }

        List<SharpeSortinoConfigEntity> enabledConfigs = configRepository.findByEnabledTrueOrderByDaysAsc().stream()
                .filter(config -> config.getDays() != null && config.getDays() > 0)
                .collect(Collectors.toList());
        if (enabledConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> alreadyGenerated = generationHistoryRepository
                .findByDateAndStatus(processingDate, SharpeSortinoGenerationStatus.SUCESS).stream()
                .map(SharpeSortinoGenerationHistoryEntity::getDays)
                .collect(Collectors.toSet());
        List<SharpeSortinoConfigEntity> configsToProcess = enabledConfigs.stream()
                .filter(config -> !alreadyGenerated.contains(config.getDays()))
                .collect(Collectors.toList());
        if (configsToProcess.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tickers = pdvCacheService.findDistinctTicker();
        List<SharpeSortinoHistoryResponse> responses = new ArrayList<>();
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        RollingSharpeSortinoPool rollingPool = new RollingSharpeSortinoPool(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (SharpeSortinoConfigEntity config : configsToProcess) {
                int days = config.getDays();
                upsertGenerationHistory(processingDate, days, SharpeSortinoGenerationStatus.IN_PROGRESS, null, null);
                log.info("Sharpe/Sortino generation marked IN_PROGRESS for date={} days={}", processingDate, days);
                Queue<SharpeSortinoResultHistoryEntity> historyQueue = new ConcurrentLinkedQueue<>();
                Set<String> skippedTickers = ConcurrentHashMap.newKeySet();

                try {
                    double configuredRatePercent = config.getRiskFreeRatePercent() == null
                            ? 6.0 : config.getRiskFreeRatePercent();
                    if (!Double.isFinite(configuredRatePercent)) {
                        throw new IllegalArgumentException("riskFreeRatePercent must be finite for days: " + days);
                    }
                    // Config values are stored as percentages (for example, 6.0 for 6%).
                    double riskFreeRateAnnual = configuredRatePercent / 100.0;

                    List<CompletableFuture<Void>> computeFutures = new ArrayList<>();
                    for (String ticker : tickers) {
                        computeFutures.add(CompletableFuture.runAsync(() -> {
                            try {
                                SharpeSortinoResultHistoryEntity row = calculateForTicker(
                                        ticker, processingDate, days, riskFreeRateAnnual, rollingPool);
                                if (row != null) {
                                    historyQueue.add(row);
                                } else {
                                    skippedTickers.add(ticker);
                                }
                            } catch (Exception e) {
                                log.warn("Sharpe/Sortino history generation failed for ticker {} on {} with window {}",
                                        ticker, processingDate, days, e);
                                skippedTickers.add(ticker);
                            }
                        }, executor));
                    }
                    CompletableFuture.allOf(computeFutures.toArray(new CompletableFuture[0])).join();

                    List<SharpeSortinoResultHistoryEntity> historyRows = new ArrayList<>(historyQueue);
                    if (!historyRows.isEmpty()) {
                        resultHistoryRepository.saveAll(historyRows);
                    }
                    String skipped = skippedTickers.isEmpty() ? null : String.join(",", skippedTickers);
                    upsertGenerationHistory(processingDate, days, SharpeSortinoGenerationStatus.SUCESS, null, skipped);
                    historyRows.stream().map(this::toResponse).forEach(responses::add);
                } catch (RuntimeException e) {
                    log.error("Sharpe/Sortino history generation failed for {} days on {}", days, processingDate, e);
                    upsertGenerationHistory(processingDate, days, SharpeSortinoGenerationStatus.FAILED,
                            safeErrorMessage(e), String.join(",", skippedTickers));
                    throw e;
                }
            }
        } finally {
            executor.shutdown();
        }
        return responses;
    }

    private SharpeSortinoResultHistoryEntity calculateForTicker(String ticker, LocalDate date, int days,
            double riskFreeRateAnnual, RollingSharpeSortinoPool rollingPool) {
        List<PriceDeliveryVolumeEntity> records = pdvCacheService.findLatestByTickerAndDate(ticker, date, days + 1);
        if (records.size() < days + 1) {
            return null;
        }

        try (RollingSharpeSortinoPool.Lease lease = rollingPool.acquire(days, riskFreeRateAnnual)) {
            RollingSharpeSortino rolling = lease.getRolling();
            double previousClose = records.get(0).getClose();
            if (!Double.isFinite(previousClose) || previousClose <= 0.0) {
                return null;
            }
            for (int i = 1; i < records.size(); i++) {
                double close = records.get(i).getClose();
                if (!Double.isFinite(close) || close <= 0.0) {
                    return null;
                }
                rolling.add((close - previousClose) / previousClose);
                previousClose = close;
            }
            if (!rolling.isReady()) {
                return null;
            }

            SharpeSortinoResultHistoryEntity entity = new SharpeSortinoResultHistoryEntity();
            entity.setTicker(ticker);
            entity.setDate(date);
            entity.setRollingDays(days);
            entity.setSortinoRatio(rolling.getSortino());
            entity.setSharpeRatio(rolling.getSharpe());
            return entity;
        }
    }

    public List<SharpeSortinoHistoryResponse> getHistory(LocalDate fromDate, LocalDate toDate, Integer days,
            List<String> tickers) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must not be before fromDate");
        }
        if (days == null || days <= 0) {
            throw new IllegalArgumentException("days must be positive");
        }

        Set<String> normalizedTickers = normalizeTickers(tickers);
        List<SharpeSortinoResultHistoryEntity> entities = normalizedTickers.isEmpty()
                ? resultHistoryRepository.findByDateBetweenAndRollingDaysOrderByDateDesc(fromDate, toDate, days)
                : resultHistoryRepository.findByDateBetweenAndRollingDaysAndTickerInOrderByDateDesc(
                        fromDate, toDate, days, normalizedTickers);
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<SharpeSortinoGenerationHistoryEntity> getGenerationHistory(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        return generationHistoryRepository.findByDate(date);
    }

    @Transactional
    public SharpeSortinoGenerationHistoryEntity updateGenerationStatus(LocalDate date, Integer days,
            SharpeSortinoGenerationStatus status) {
        if (date == null || days == null || days <= 0 || status == null) {
            throw new IllegalArgumentException("date, positive days, and status are required");
        }
        SharpeSortinoGenerationHistoryId id = new SharpeSortinoGenerationHistoryId(date, days);
        SharpeSortinoGenerationHistoryEntity entity = generationHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Generation history not found for date=" + date + " days=" + days));
        entity.setStatus(status);
        if (status == SharpeSortinoGenerationStatus.SUCESS || status == SharpeSortinoGenerationStatus.FAILED) {
            entity.setErrorMessage(null);
        }
        return generationHistoryRepository.save(entity);
    }

    private Set<String> normalizeTickers(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) {
            return Set.of();
        }
        return tickers.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private void upsertGenerationHistory(LocalDate date, Integer days, SharpeSortinoGenerationStatus status,
            String errorMessage, String skippedTickers) {
        SharpeSortinoGenerationHistoryEntity entity = new SharpeSortinoGenerationHistoryEntity();
        entity.setDate(date);
        entity.setDays(days);
        entity.setStatus(status);
        entity.setErrorMessage(errorMessage);
        entity.setSkippedTickers(skippedTickers);
        generationHistoryRepository.save(entity);
    }

    private String safeErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() > 1024 ? message.substring(0, 1024) : message;
    }

    private SharpeSortinoHistoryResponse toResponse(SharpeSortinoResultHistoryEntity entity) {
        SharpeSortinoHistoryResponse response = new SharpeSortinoHistoryResponse();
        response.setTicker(entity.getTicker());
        response.setDate(entity.getDate());
        response.setRollingDays(entity.getRollingDays());
        response.setSortinoRatio(entity.getSortinoRatio());
        response.setSharpeRatio(entity.getSharpeRatio());
        return response;
    }
}
