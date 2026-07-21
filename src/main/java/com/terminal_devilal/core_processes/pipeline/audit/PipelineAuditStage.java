package com.terminal_devilal.core_processes.pipeline.audit;

import java.util.Locale;

public enum PipelineAuditStage {
    API_FETCH,
    API_RECEIVED,
    PARSE,
    PDVT_SAVE,
    TRADEINFO_SAVE,
    DFHT_UPDATE,
    KAFKA_PUBLISH,
    KAFKA_CONSUME,
    ATR_PROCESS,
    ATR_SAVE,
    RSI_PROCESS,
    RSI_SAVE,
    VWAP_PROCESS,
    VWAP_SAVE,
    BATCH_FLUSH,
    ERROR,
    TICKER_SUMMARY;

    public String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
