package com.terminal_devilal.indicators.vwap.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.common_entities.TickerDateId;
import com.terminal_devilal.indicators.vwap.entity.VWAPEntity;
import com.terminal_devilal.indicators.vwap.entity.projections.VwapProjection;
import com.terminal_devilal.indicators.vwap.repository.VWAPRepository;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineTickerContext;
import com.terminal_devilal.utils.resilientbatchservice.ResilientBatchService;

import jakarta.transaction.Transactional;

@Service
public class VWAPService extends ResilientBatchService<VWAPEntity> {

    private final VWAPRepository vWAPRepository;

    public VWAPService(VWAPRepository vWAPRepository, PipelineAuditService pipelineAuditService) {
        super(pipelineAuditService);
        this.vWAPRepository = vWAPRepository;
    }

    @Transactional
    public void saveAllVWAP(List<VWAPEntity> vwaps) {
        this.vWAPRepository.saveAll(vwaps);
    }

    @Transactional
    public void saveVWAP(VWAPEntity vWAPEntity) {
        this.vWAPRepository.save(vWAPEntity);
    }

    public List<VwapProjection> getVwapDataWithinDates(List<String> tickers, LocalDate fromDate, LocalDate toDate) {
        return vWAPRepository.findByTickerInAndDateBetween(tickers, fromDate, toDate);
    }

    public void processVwap(PriceDeliveryVolumeEntity pdv, PipelineTickerContext context) {
        if (pdv.getVwap() == 0) {
            pipelineAuditService.logEvent(context.getRunId(), pdv.getTicker(), PipelineAuditStage.VWAP_PROCESS,
                    "SKIP", 0, pdv.getDate().toString(), pdv.getDate().toString(), null, "VWAP is zero", null, null);
            return;
        }

        TickerDateId id = new TickerDateId(pdv.getTicker(), pdv.getDate());
        if (vWAPRepository.existsById(id)) {
            pipelineAuditService.logEvent(context, PipelineAuditStage.VWAP_PROCESS, "SKIP", 0,
                    pdv.getDate().toString(), pdv.getDate().toString(), null,
                    "VWAP already exists for ticker/date", null, null);
            return;
        }

        double vwapProximity = (pdv.getClose() - pdv.getVwap()) / pdv.getVwap() * 100;
        VWAPEntity vWAPEntity = new VWAPEntity(pdv.getTicker(), pdv.getDate(), pdv.getClose(), pdv.getVwap(), vwapProximity);
        context.getMetrics().incrementVwapProcessed();
        pipelineAuditService.logStageStart(context, PipelineAuditStage.VWAP_PROCESS, "VWAP processing started");
        enqueue(vWAPEntity);
        pipelineAuditService.logStageSuccess(context, PipelineAuditStage.VWAP_PROCESS, 1, pdv.getDate().toString(), pdv.getDate().toString(), null, "VWAP queued");
    }

    @Override
    @Transactional
    protected void saveAll(List<VWAPEntity> batch) {
        for (VWAPEntity entity : batch) {
            vWAPRepository.upsert(entity.getTicker(), entity.getDate(), entity.getClosePrice(), entity.getVwap(),
                    entity.getVwapProximity());
        }
    }

    @Override
    @Transactional
    protected void saveOne(VWAPEntity record) {
        vWAPRepository.upsert(record.getTicker(), record.getDate(), record.getClosePrice(), record.getVwap(),
                record.getVwapProximity());
    }

    @Override
    protected void onPermanentFailure(VWAPEntity record, Exception e) {
        pipelineAuditService.logEvent(null, record.getTicker(), PipelineAuditStage.VWAP_SAVE,
                "FAILURE", 1, record.getDate().toString(), record.getDate().toString(), null,
                "VWAP persistence failed", e.getMessage(), null);
    }
}
