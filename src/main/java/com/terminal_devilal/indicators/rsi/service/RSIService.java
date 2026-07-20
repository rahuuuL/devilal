package com.terminal_devilal.indicators.rsi.service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.common_entities.TickerDateId;
import com.terminal_devilal.indicators.rsi.dto.RsiPercentileDTO;
import com.terminal_devilal.indicators.rsi.entities.RSIEntity;
import com.terminal_devilal.indicators.rsi.entities.projections.RsiPercentileProjection;
import com.terminal_devilal.indicators.rsi.entities.projections.RsiProjection;
import com.terminal_devilal.indicators.rsi.repository.RSIRepository;
import com.terminal_devilal.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.pipeline.audit.PipelineTickerContext;
import com.terminal_devilal.utils.common_calcs.PercentileCalculator;
import com.terminal_devilal.utils.resilientbatchservice.ResilientBatchService;

import jakarta.transaction.Transactional;

@Service
public class RSIService extends ResilientBatchService<RSIEntity> {

    private final RSIRepository rSIRepository;
    private final ConcurrentHashMap<String, ArrayDeque<RSIEntity>> rsiCache = new ConcurrentHashMap<>();
    private static final int CACHE_MAX = 21;

    public RSIService(RSIRepository rSIRepository, PipelineAuditService pipelineAuditService) {
        super(pipelineAuditService);
        this.rSIRepository = rSIRepository;
    }

    @Transactional
    public void saveAllRSI(List<RSIEntity> rsis) {
        this.rSIRepository.saveAll(rsis);
    }

    @Transactional
    public void saveRSI(RSIEntity rsis) {
        this.rSIRepository.save(rsis);
    }

    public List<RSIEntity> getAllRSIByDate(LocalDate date) {
        return rSIRepository.findByDate(date);
    }

    public List<RsiProjection> getRSIWithinDatesForTickers(List<String> tickers, LocalDate fromDate, LocalDate toDate) {
        return rSIRepository.findByTickerInAndDateBetween(tickers, fromDate, toDate);
    }

    public void processRSI(PriceDeliveryVolumeEntity pdv, PipelineTickerContext context) {
        String ticker = pdv.getTicker();
        TickerDateId id = new TickerDateId(ticker, pdv.getDate());
        if (rSIRepository.existsById(id)) {
            pipelineAuditService.logEvent(context, PipelineAuditStage.RSI_PROCESS, "SKIP", 0,
                    pdv.getDate().toString(), pdv.getDate().toString(), null,
                    "RSI already exists for ticker/date", null, null);
            return;
        }

        double closeDiff = pdv.getClose() - pdv.getPrevoiusClosePrice();

        ArrayDeque<RSIEntity> window = rsiCache.computeIfAbsent(ticker, t -> {
            List<RSIEntity> dbData = rSIRepository.findRecent21RSIs(ticker, pdv.getDate());
            return new ArrayDeque<>(dbData);
        });

        RSIEntity newEntity;

        synchronized (window) {
            List<RSIEntity> snapshot = new ArrayList<>(window);
            int size = snapshot.size();

            double rsi14 = size >= 14 ? calculateRSI(snapshot.subList(size - 14, size)) : 0;
            double rsi21 = size == CACHE_MAX ? calculateRSI(snapshot) : 0;

            newEntity = new RSIEntity(ticker, pdv.getDate(), closeDiff, rsi14, rsi21);

            if (window.size() >= CACHE_MAX) {
                window.pollFirst();
            }
            window.addLast(newEntity);
        }
        context.getMetrics().incrementRsiProcessed();
        pipelineAuditService.logStageStart(context, PipelineAuditStage.RSI_PROCESS, "RSI processing started");
        enqueue(newEntity);
        pipelineAuditService.logStageSuccess(context, PipelineAuditStage.RSI_PROCESS, 1, pdv.getDate().toString(), pdv.getDate().toString(), null, "RSI queued");
    }

    private double calculateRSI(List<RSIEntity> rsiData) {
        double gainSum = 0.0;
        double lossSum = 0.0;

        for (RSIEntity data : rsiData) {
            double change = data.getCloseDiff();
            if (change > 0) {
                gainSum += change;
            } else if (change < 0) {
                lossSum += Math.abs(change);
            }
        }

        double averageGain = gainSum / rsiData.size();
        double averageLoss = lossSum / rsiData.size();

        if (averageLoss == 0) {
            return 100.0;
        }

        double rs = averageGain / averageLoss;
        return 100.0 - (100.0 / (1 + rs));
    }

    @Override
    @Transactional
    protected void saveAll(List<RSIEntity> batch) {
        for (RSIEntity entity : batch) {
            rSIRepository.upsert(entity.getTicker(), entity.getDate(), entity.getCloseDiff(),
                    entity.getFourtheenDaysRSI(), entity.getTweentyOneDaysRSI());
        }
    }

    @Override
    @Transactional
    protected void saveOne(RSIEntity record) {
        rSIRepository.upsert(record.getTicker(), record.getDate(), record.getCloseDiff(),
                record.getFourtheenDaysRSI(), record.getTweentyOneDaysRSI());
    }

    @Override
    protected void onPermanentFailure(RSIEntity record, Exception e) {
        pipelineAuditService.logEvent(null, record.getTicker(), PipelineAuditStage.RSI_SAVE,
                "FAILURE", 1, record.getDate().toString(), record.getDate().toString(), null,
                "RSI persistence failed", e.getMessage(), null);
    }

    public List<RsiPercentileDTO> computeRsiPercentiles(LocalDate fromDate, LocalDate toDate, boolean use14DayRsi) {

        List<RsiPercentileProjection> rows = rSIRepository.findPercentileRecordsBetweenDates(fromDate, toDate);

        Map<String, List<RsiPercentileProjection>> byTicker = rows.stream()
                .collect(Collectors.groupingBy(RsiPercentileProjection::getTicker));

        List<RsiPercentileDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<RsiPercentileProjection>> entry : byTicker.entrySet()) {

            String ticker = entry.getKey();
            List<RsiPercentileProjection> data = entry.getValue();

            Optional<RsiPercentileProjection> endDateRow = data.stream().filter(d -> d.getDate().equals(toDate))
                    .findFirst();

            if (endDateRow.isEmpty()) {
                continue;
            }

            double targetRsi = use14DayRsi ? endDateRow.get().getFourtheenDaysRSI()
                    : endDateRow.get().getTweentyOneDaysRSI();

            List<Double> rsiSeries = data.stream()
                    .map(d -> use14DayRsi ? d.getFourtheenDaysRSI() : d.getTweentyOneDaysRSI()).sorted().toList();

            double percentile = PercentileCalculator.computePercentileSorted(rsiSeries, targetRsi);

            RsiPercentileDTO dto = new RsiPercentileDTO();
            dto.setTicker(ticker);
            dto.setRsiValue(targetRsi);
            dto.setPercentile(percentile);

            result.add(dto);
        }

        return result;
    }
}
