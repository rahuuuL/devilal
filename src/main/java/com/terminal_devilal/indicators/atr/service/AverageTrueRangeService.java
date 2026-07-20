package com.terminal_devilal.indicators.atr.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.indicators.atr.entities.AverageTrueRangeEntity;
import com.terminal_devilal.indicators.atr.entities.projections.TrueRangeProjection;
import com.terminal_devilal.indicators.atr.repository.AverageTrueRangeRepository;
import com.terminal_devilal.indicators.common_entities.TickerDateId;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.pipeline.audit.PipelineTickerContext;
import com.terminal_devilal.utils.resilientbatchservice.ResilientBatchService;

import jakarta.transaction.Transactional;

@Service
public class AverageTrueRangeService extends ResilientBatchService<AverageTrueRangeEntity> {

    private final AverageTrueRangeRepository averageTrueRangeRepository;

    public AverageTrueRangeService(AverageTrueRangeRepository averageTrueRangeRepository,
            PipelineAuditService pipelineAuditService) {
        super(pipelineAuditService);
        this.averageTrueRangeRepository = averageTrueRangeRepository;
    }

    @Transactional
    public void saveAllATR(List<AverageTrueRangeEntity> averageTrueRangeEntities) {
        this.averageTrueRangeRepository.saveAll(averageTrueRangeEntities);
    }

    @Transactional
    public void saveATR(AverageTrueRangeEntity averageTrueRangeEntities) {
        this.averageTrueRangeRepository.save(averageTrueRangeEntities);
    }

    public List<TrueRangeProjection> getLastNRecordsPerTicker(List<String> tickers, int n) {
        return averageTrueRangeRepository.findLastNRecordsPerTicker(tickers, n);
    }

    public double calculateTrueRange(double high, double low, double prevClose) {
        double range1 = high - low;
        double range2 = Math.abs(high - prevClose);
        double range3 = Math.abs(low - prevClose);
        return Math.max(range1, Math.max(range2, range3));
    }

    public void processATR(PriceDeliveryVolumeEntity pdv, PipelineTickerContext context) {
        TickerDateId id = new TickerDateId(pdv.getTicker(), pdv.getDate());
        if (averageTrueRangeRepository.existsById(id)) {
            pipelineAuditService.logEvent(context, PipelineAuditStage.ATR_PROCESS, "SKIP", 0,
                    pdv.getDate().toString(), pdv.getDate().toString(), null,
                    "ATR already exists for ticker/date", null, null);
            return;
        }

        double trueRange = calculateTrueRange(pdv.getHigh(), pdv.getLow(), pdv.getPrevoiusClosePrice());
        AverageTrueRangeEntity newEntity = new AverageTrueRangeEntity(pdv.getTicker(), pdv.getDate(), trueRange);
        context.getMetrics().incrementAtrProcessed();
        pipelineAuditService.logStageStart(context, PipelineAuditStage.ATR_PROCESS, "ATR processing started");
        enqueue(newEntity);
        pipelineAuditService.logStageSuccess(context, PipelineAuditStage.ATR_PROCESS, 1, pdv.getDate().toString(), pdv.getDate().toString(), null, "ATR queued");
    }

    @Override
    @Transactional
    protected void saveAll(List<AverageTrueRangeEntity> batch) {
        for (AverageTrueRangeEntity entity : batch) {
            averageTrueRangeRepository.upsert(entity.getTicker(), entity.getDate(), entity.getTrueRange());
        }
    }

    @Override
    @Transactional
    protected void saveOne(AverageTrueRangeEntity record) {
        averageTrueRangeRepository.upsert(record.getTicker(), record.getDate(), record.getTrueRange());
    }

    @Override
    protected void onPermanentFailure(AverageTrueRangeEntity record, Exception e) {
        pipelineAuditService.logEvent(null, record.getTicker(), PipelineAuditStage.ATR_SAVE,
                "FAILURE", 1, record.getDate().toString(), record.getDate().toString(), null,
                "ATR persistence failed", e.getMessage(), null);
    }
}
