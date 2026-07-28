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

import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallAPIResponse;
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
    public List<MkResultHistoryEntity> generateHistory(LocalDate processingDate) {
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

        List<MkResultHistoryEntity> generatedRecords = new ArrayList<>();

        for (MkConfigEntity config : configsToProcess) {

            try {
                WorkingDayDateRangeUtil.DateRange range = WorkingDayDateRangeUtil.calculateDateRange(processingDate,
                        config.getDays());
                List<MannKendallAPIResponse> results = analyzeMannKendallForTicker.getMannKendallTrendAnalysis(
                        range.getFromDate(), processingDate);
                List<MkResultHistoryEntity> recordsForWindow = mapToEntities(results, processingDate, config.getDays());

                // Re-run safety: remove already persisted data for this date+window before inserting.
                mkResultHistoryRepository.deleteByDateAndDays(processingDate, config.getDays());
                if (!recordsForWindow.isEmpty()) {
                    mkResultHistoryRepository.saveAll(recordsForWindow);
                }

                generatedRecords.addAll(recordsForWindow);
                upsertGenerationHistory(processingDate, config.getDays(), MkGenerationStatus.SUCESS, null);
            } catch (Exception e) {
                log.warn("MK history generation failed for days {}", config.getDays(), e);
                upsertGenerationHistory(processingDate, config.getDays(), MkGenerationStatus.FAILED, safeErrorMessage(e));
            }
        }

        return generatedRecords;
    }

    public List<MkResultHistoryEntity> getHistory(LocalDate date, Integer days, List<String> tickers) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        if (tickers == null || tickers.isEmpty()) {
            return mkResultHistoryRepository.findHistory(date, days, null);
        }

        Set<String> normalizedTickers = tickers.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        return mkResultHistoryRepository.findHistory(date, days, normalizedTickers.isEmpty() ? null : normalizedTickers);
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

    private List<MkResultHistoryEntity> mapToEntities(List<MannKendallAPIResponse> responses, LocalDate processingDate,
            Integer days) {
        List<MkResultHistoryEntity> records = new ArrayList<>();
        for (MannKendallAPIResponse response : responses) {
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
