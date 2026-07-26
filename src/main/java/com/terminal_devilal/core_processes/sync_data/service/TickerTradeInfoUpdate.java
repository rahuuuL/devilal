package com.terminal_devilal.core_processes.sync_data.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.business_tools.trade_info.entity.TradeInfo;
import com.terminal_devilal.business_tools.trade_info.service.TradeInfoService;
import com.terminal_devilal.core_processes.dfht.entity.DataFetchEntity;
import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;
import com.terminal_devilal.utils.nse.FetchNSEAPI;

@Service
public class TickerTradeInfoUpdate {

    private static final Logger log = LoggerFactory.getLogger(TickerTradeInfoUpdate.class);

    private static final int THREAD_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors());
    private static final int MAX_API_CONCURRENCY = Math.max(1, Math.min(THREAD_POOL_SIZE, 8));

    private final FetchNSEAPI fetchNSEAPI;
    private final DataFetchHistoryService dataFetchHistoryService;
    private final TradeInfoService tradeInfoService;

    private final Semaphore apiLimiter = new Semaphore(MAX_API_CONCURRENCY);
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TickerTradeInfoUpdate(FetchNSEAPI fetchNSEAPI,
            DataFetchHistoryService dataFetchHistoryService,
            TradeInfoService tradeInfoService) {
        this.fetchNSEAPI = fetchNSEAPI;
        this.dataFetchHistoryService = dataFetchHistoryService;
        this.tradeInfoService = tradeInfoService;
    }

    public void updateTradeInfoData() {
        if (!running.compareAndSet(false, true)) {
            log.warn("TradeInfo sync already running");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try {
            List<DataFetchEntity> records = dataFetchHistoryService.getProcessedDatesForTickers();
            log.info("TradeInfo sync started for {} tickers", records.size());

            for (DataFetchEntity record : records) {
                executor.submit(() -> processSingleTicker(record));
            }
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            running.set(false);
            log.info("TradeInfo sync completed");
        }
    }

    private void processSingleTicker(DataFetchEntity record) {
        String symbol = record.getTicker();

        try {
            apiLimiter.acquire();

            JsonNode response = fetchNSEAPI.NSEAPICall(fetchNSEAPI.buildTradeInfoUrl(symbol));
            Optional<TradeInfo> tradeInfo = tradeInfoService.parseTradeInfo(response, symbol, LocalDate.now());

            if (tradeInfo.isEmpty()) {
                log.warn("No TradeInfo payload for {}", symbol);
                return;
            }

            tradeInfoService.saveTradeInfo(tradeInfo.get());
            log.info("Saved TradeInfo for {}", symbol);
        } catch (Exception ex) {
            log.error("Failed TradeInfo sync for {} : {}", symbol, ex.getMessage(), ex);
        } finally {
            apiLimiter.release();
        }
    }
}
