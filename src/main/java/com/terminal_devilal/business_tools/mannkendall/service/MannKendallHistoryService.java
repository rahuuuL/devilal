package com.terminal_devilal.business_tools.mannkendall.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

        List<MkResultHistoryEntity> generatedEntities = new ArrayList<>();

        for (MkConfigEntity config : configsToProcess) {

            try {
                WorkingDayDateRangeUtil.DateRange range = WorkingDayDateRangeUtil.calculateDateRange(processingDate,
                        config.getDays());
                List<MannKendallResponse> results = analyzeMannKendallForTicker.getMannKendallTrendAnalysis(
                        range.getFromDate(), processingDate);
                List<MkResultHistoryEntity> recordsForWindow = mapToEntities(results, processingDate, config.getDays());

                // Re-run safety: remove already persisted data for this date+window before inserting.
                mkResultHistoryRepository.deleteByDateAndDays(processingDate, config.getDays());
                if (!recordsForWindow.isEmpty()) {
                    mkResultHistoryRepository.saveAll(recordsForWindow);
                }

                generatedEntities.addAll(recordsForWindow);
                upsertGenerationHistory(processingDate, config.getDays(), MkGenerationStatus.SUCESS, null);
            } catch (Exception e) {
                log.warn("MK history generation failed for days {}", config.getDays(), e);
                upsertGenerationHistory(processingDate, config.getDays(), MkGenerationStatus.FAILED, safeErrorMessage(e));
            }
        }

        return toResponses(generatedEntities);
    }

    public List<MkResultHistoryResponse> getHistory(LocalDate date, Integer days, List<String> tickers) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        Set<String> normalizedTickers = normalizeTickers(tickers);
        boolean hasTickers = !normalizedTickers.isEmpty();
        boolean hasDays = days != null && days > 0;

        if (!hasDays && !hasTickers) {
            return toResponses(fetchByDateOnly(date));
        }

        if (hasDays && !hasTickers) {
            return toResponses(fetchByDateAndDays(date, days));
        }

        if (!hasDays) {
            return toResponses(fetchByDateAndTickers(date, normalizedTickers));
        }

        return toResponses(fetchByDateDaysAndTickers(date, days, normalizedTickers));
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

    private List<MkResultHistoryEntity> fetchByDateOnly(LocalDate date) {
        return mkResultHistoryRepository.findByDate(date);
    }

    private List<MkResultHistoryEntity> fetchByDateAndDays(LocalDate date, Integer days) {
        return mkResultHistoryRepository.findByDateAndDays(date, days);
    }

    private List<MkResultHistoryEntity> fetchByDateAndTickers(LocalDate date, Set<String> tickers) {
        return mkResultHistoryRepository.findByDateAndTickerIn(date, tickers);
    }

    private List<MkResultHistoryEntity> fetchByDateDaysAndTickers(LocalDate date, Integer days, Set<String> tickers) {
        return mkResultHistoryRepository.findByDateAndDaysAndTickerIn(date, days, tickers);
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
