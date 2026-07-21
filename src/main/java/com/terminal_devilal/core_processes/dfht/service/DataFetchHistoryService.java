package com.terminal_devilal.core_processes.dfht.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.core_processes.dfht.entity.DataFetchEntity;
import com.terminal_devilal.core_processes.dfht.repository.DataFetchHistroyRepository;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineTickerContext;

import jakarta.transaction.Transactional;

@Service
public class DataFetchHistoryService {

    private final DataFetchHistroyRepository repository;
    private final PipelineAuditService pipelineAuditService;

    public DataFetchHistoryService(DataFetchHistroyRepository repository, PipelineAuditService pipelineAuditService) {
        this.repository = repository;
        this.pipelineAuditService = pipelineAuditService;
    }

    public List<DataFetchEntity> getProcessedDatesForTickers() {
        return repository.findAll();
    }

    public List<String> getAllTickers() {
        return repository.findAllTickers();
    }

    @Transactional
    public void updateLastDateForPdvt(String ticker, LocalDate date) {
        updateLastDateForPdvt(ticker, date, null);
    }

    @Transactional
    public void updateLastDateForPdvt(String ticker, LocalDate date, PipelineTickerContext tickerContext) {
        repository.updateLastDate(ticker, date);
        if (tickerContext != null) {
            pipelineAuditService.logStageSuccess(tickerContext, PipelineAuditStage.DFHT_UPDATE, 1,
                    date.toString(), date.toString(), null, "DFHT last date updated");
        }
    }
}
