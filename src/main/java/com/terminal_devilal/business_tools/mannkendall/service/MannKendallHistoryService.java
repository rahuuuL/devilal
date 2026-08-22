package com.terminal_devilal.business_tools.mannkendall.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallResponse;
import com.terminal_devilal.business_tools.mannkendall.dto.MkResultHistoryResponse;
import com.terminal_devilal.business_tools.mannkendall.entity.MkConfigEntity;
import com.terminal_devilal.business_tools.mannkendall.entity.MkGenerationHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.entity.MkGenerationHistoryId;
import com.terminal_devilal.business_tools.mannkendall.entity.MkGenerationStatus;
import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.repository.MkConfigRepository;
import com.terminal_devilal.business_tools.mannkendall.repository.MkGenerationHistoryRepository;
import com.terminal_devilal.business_tools.mannkendall.repository.MkResultHistoryRepository;
import com.terminal_devilal.utils.WorkingDayDateRangeUtil;

import io.micrometer.core.annotation.Timed;

@Service
public class MannKendallHistoryService {

    private static final Logger log = LoggerFactory.getLogger(MannKendallHistoryService.class);

    private final MkConfigRepository mkConfigRepository;
    private final MkGenerationHistoryRepository mkGenerationHistoryRepository;
    private final MkResultHistoryRepository mkResultHistoryRepository;
    private final AnalyzeMannKendallForTicker analyzeMannKendallForTicker;

    public MannKendallHistoryService(MkConfigRepository mkConfigRepository,
            MkGenerationHistoryRepository mkGenerationHistoryRepository,
            MkResultHistoryRepository mkResultHistoryRepository,
            AnalyzeMannKendallForTicker analyzeMannKendallForTicker) {
        this.mkConfigRepository = mkConfigRepository;
        this.mkGenerationHistoryRepository = mkGenerationHistoryRepository;
        this.mkResultHistoryRepository = mkResultHistoryRepository;
        this.analyzeMannKendallForTicker = analyzeMannKendallForTicker;
    }

    @Transactional
    @Timed(
        value = "mk.history.generate",
        description = "MK history generation"
    )
    public List<MkResultHistoryResponse> generateHistory(LocalDate processingDate) {
        if (processingDate == null) {
            throw new IllegalArgumentException("processingDate must not be null");
        }

        List<MkConfigEntity> configs = mkConfigRepository.findByEnabledTrueOrderByDaysAsc();
        Set<Integer> successfulDays = mkGenerationHistoryRepository
            .findByDateAndStatus(processingDate, MkGenerationStatus.SUCESS)
                .stream()
                .map(MkGenerationHistoryEntity::getDays)
                .collect(Collectors.toSet());

        List<MkConfigEntity> configsToProcess = configs.stream()
                .filter(config -> config.getDays() != null && config.getDays() > 0)
                .filter(config -> !successfulDays.contains(config.getDays()))
                .collect(Collectors.toList());

        List<MkResultHistoryEntity> allRecords = new ArrayList<>();

        List<Integer> daysToProcess = configsToProcess.stream().map(MkConfigEntity::getDays).collect(Collectors.toList());
        AnalyzeMannKendallForTicker.WindowAnalysisBatchResult batchResult = analyzeMannKendallForTicker
                .getMannKendallTrendAnalysisByDays(processingDate, daysToProcess);
        Map<Integer, List<MannKendallResponse>> resultsByDays = batchResult.getResultsByDays();
        Map<Integer, String> errorsByDays = batchResult.getErrorsByDays();

        for (MkConfigEntity config : configsToProcess) {
            Integer days = config.getDays();

            String analysisError = errorsByDays.get(days);
            if (analysisError != null) {
                upsertGenerationHistory(processingDate, days, MkGenerationStatus.FAILED, analysisError);
                continue;
            }

            try {
                List<MannKendallResponse> results = resultsByDays.getOrDefault(days, Collections.emptyList());

                List<MkResultHistoryEntity> recordsForWindow = mapToEntities(results, processingDate, days);

                allRecords.addAll(recordsForWindow);

                upsertGenerationHistory(processingDate, days, MkGenerationStatus.SUCESS, null);
            } catch (Exception e) {
                log.warn("MK history generation failed for days {}", days, e);
                upsertGenerationHistory(processingDate, days, MkGenerationStatus.FAILED, safeErrorMessage(e));
            }
        }

        if (!allRecords.isEmpty()) {
            mkResultHistoryRepository.upsertBatch(allRecords);
        }

        return toResponses(allRecords);
    }

    public List<MkResultHistoryResponse> getHistory(LocalDate fromDate, LocalDate toDate, Integer days,
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

        Set<String> normalizedTickers = normalizeTickers(tickers);
        boolean hasTickers = !normalizedTickers.isEmpty();
        boolean hasDays = days != null && days > 0;

        if (!hasDays && !hasTickers) {
            return toResponses(fetchByDateRange(fromDate, toDate));
        }

        if (hasDays && !hasTickers) {
            return toResponses(fetchByDateRangeAndDays(fromDate, toDate, days));
        }

        if (!hasDays) {
            return toResponses(fetchByDateRangeAndTickers(fromDate, toDate, normalizedTickers));
        }

        return toResponses(fetchByDateRangeDaysAndTickers(fromDate, toDate, days, normalizedTickers));
    }

    public List<MkGenerationHistoryEntity> getGenerationHistory(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        return mkGenerationHistoryRepository.findByDate(date);
    }

    @Transactional
    public MkGenerationHistoryEntity updateGenerationStatus(LocalDate date, Integer days, MkGenerationStatus status) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        if (days == null || days <= 0) {
            throw new IllegalArgumentException("days must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        MkGenerationHistoryId id = new MkGenerationHistoryId(date, days);
        MkGenerationHistoryEntity entity = mkGenerationHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Generation history not found for date=" + date + " days=" + days));

        entity.setStatus(status);
        if (status == MkGenerationStatus.SUCESS) {
            entity.setErrorMessage(null);
        }
        return mkGenerationHistoryRepository.save(entity);
    }

    private List<MkResultHistoryEntity> mapToEntities(List<MannKendallResponse> responses, LocalDate processingDate,
            Integer days) {
        List<MkResultHistoryEntity> records = new ArrayList<>();
        for (MannKendallResponse response : responses) {
            if (response == null || response.getTicker() == null) {
                continue;
            }
            MkResultHistoryEntity entity = new MkResultHistoryEntity();
            entity.setTicker(response.getTicker());
            entity.setDate(processingDate);
            entity.setDays(days);
            entity.setScore(response.getScore());
            entity.setTrend(response.getTrend());
            entity.setH(response.getH());
            entity.setP(response.getP());
            entity.setZ(response.getZ());
            entity.setTau(response.getTau());
            entity.setS(response.getS());
            entity.setVar_s(response.getVar_s());
            entity.setSlope(response.getSlope());
            entity.setIntercept(response.getIntercept());
            records.add(entity);
        }
        return records;
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

    private List<MkResultHistoryEntity> fetchByDateRange(LocalDate fromDate, LocalDate toDate) {
        return mkResultHistoryRepository.findByDateBetweenOrderByDateDesc(fromDate, toDate);
    }

    private List<MkResultHistoryEntity> fetchByDateRangeAndDays(LocalDate fromDate, LocalDate toDate, Integer days) {
        return mkResultHistoryRepository.findByDateBetweenAndDaysOrderByDateDesc(fromDate, toDate, days);
    }

    private List<MkResultHistoryEntity> fetchByDateRangeAndTickers(LocalDate fromDate, LocalDate toDate,
            Set<String> tickers) {
        return mkResultHistoryRepository.findByDateBetweenAndTickerInOrderByDateDesc(fromDate, toDate, tickers);
    }

    private List<MkResultHistoryEntity> fetchByDateRangeDaysAndTickers(LocalDate fromDate, LocalDate toDate,
            Integer days, Set<String> tickers) {
        return mkResultHistoryRepository.findByDateBetweenAndDaysAndTickerInOrderByDateDesc(fromDate, toDate, days,
                tickers);
    }

    private List<MkResultHistoryResponse> toResponses(List<MkResultHistoryEntity> entities) {
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private MkResultHistoryResponse toResponse(MkResultHistoryEntity entity) {
        MkResultHistoryResponse response = new MkResultHistoryResponse();
        response.setTicker(entity.getTicker());
        response.setDate(entity.getDate());
        response.setDays(entity.getDays());
        response.setScore(entity.getScore());
        response.setTrend(entity.getTrend());
        response.setH(entity.getH());
        response.setP(entity.getP());
        response.setZ(entity.getZ());
        response.setTau(entity.getTau());
        response.setS(entity.getS());
        response.setVar_s(entity.getVar_s());
        response.setSlope(entity.getSlope());
        response.setIntercept(entity.getIntercept());
        return response;
    }

    private void upsertGenerationHistory(LocalDate date, Integer days, MkGenerationStatus status, String errorMessage) {
        MkGenerationHistoryEntity history = new MkGenerationHistoryEntity();
        history.setDate(date);
        history.setDays(days);
        history.setStatus(status);
        history.setErrorMessage(errorMessage);
        mkGenerationHistoryRepository.save(history);
    }

    private String safeErrorMessage(Exception e) {
        if (e == null) {
            return null;
        }
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
