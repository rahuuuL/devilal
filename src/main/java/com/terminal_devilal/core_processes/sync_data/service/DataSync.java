package com.terminal_devilal.core_processes.sync_data.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.business_tools.trade_info.entities.TradeInfo;
import com.terminal_devilal.business_tools.trade_info.service.TradeInfoService;
import com.terminal_devilal.core_processes.dfht.entities.DataFetchEntity;
import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.pipeline.audit.PipelineRunContext;
import com.terminal_devilal.pipeline.audit.PipelineTickerContext;
import com.terminal_devilal.utils.nse.FetchNSEAPI;

@Service
public class DataSync {

    private static final Logger log = LoggerFactory.getLogger(DataSync.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final int API_THREADS = 8;
    private static final int PROCESS_THREADS = Runtime.getRuntime().availableProcessors();
    private static final int QUEUE_SIZE = 10000;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final DataFetchHistoryService dataFetchHistoryService;
    private final PriceDeliveryVolumeService priceDeliveryVolumeService;
    private final FetchNSEAPI fetchNSEAPI;
    private final PdvPersistenceService pdvPersistenceService;
    private final TradeInfoService tradeInfoService;
    private final PipelineAuditService pipelineAuditService;

    public DataSync(DataFetchHistoryService dataFetchHistoryService,
            PriceDeliveryVolumeService priceDeliveryVolumeService, FetchNSEAPI fetchNSEAPI,
            PdvPersistenceService pdvPersistenceService, TradeInfoService tradeInfoService,
            PipelineAuditService pipelineAuditService) {

        this.dataFetchHistoryService = dataFetchHistoryService;
        this.priceDeliveryVolumeService = priceDeliveryVolumeService;
        this.fetchNSEAPI = fetchNSEAPI;
        this.pdvPersistenceService = pdvPersistenceService;
        this.tradeInfoService = tradeInfoService;
        this.pipelineAuditService = pipelineAuditService;
    }

    public void processPdvDataTillDate() {

        if (!running.compareAndSet(false, true)) {
            log.warn("PDV sync already running. Skipping.");
            return;
        }

        PipelineRunContext runContext = pipelineAuditService.startRun();
        BlockingQueue<WorkItem> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);

        ExecutorService apiExecutor = Executors.newFixedThreadPool(API_THREADS);
        ExecutorService processExecutor = Executors.newFixedThreadPool(PROCESS_THREADS);

        try {
            List<DataFetchEntity> records = dataFetchHistoryService.getProcessedDatesForTickers().stream()
                    .filter(d -> d.getPdvtLastDate().isBefore(LocalDate.now())).toList();

            int total = records.size();
            AtomicInteger completed = new AtomicInteger();

            log.info("Starting PDV sync for {} tickers", total);

            for (int i = 0; i < PROCESS_THREADS; i++) {
                processExecutor.submit(() -> {
                    while (true) {
                        try {
                            WorkItem item = queue.poll(10, TimeUnit.SECONDS);
                            if (item == null) {
                                break;
                            }

                            processAndPersist(item, runContext);

                            int done = completed.incrementAndGet();
                            log.info("Progress: {:.2f}% ({}/{})", (100.0 * done) / total, done, total);

                        } catch (Exception e) {
                            log.error("Pipeline worker failure", e);
                        }
                    }
                });
            }

            for (DataFetchEntity data : records) {
                apiExecutor.submit(() -> {
                    try {
                        fetchWithRetryAndQueue(data, queue, runContext);
                    } catch (Exception e) {
                        log.error("API error for ticker {}", data.getTicker(), e);
                    }
                });
            }

        } finally {
            shutdown(apiExecutor);
            shutdown(processExecutor);
            running.set(false);
            log.info("PDV sync finished");
        }
    }

    private void processAndPersist(WorkItem item, PipelineRunContext runContext) {
        DataFetchEntity data = item.data;
        JsonNode pdvResponse = item.response;
        PipelineTickerContext tickerContext = pipelineAuditService.startTickerContext(runContext.getRunId(), data.getTicker());

        long startedAt = System.currentTimeMillis();
        pipelineAuditService.logStageStart(tickerContext, PipelineAuditStage.PARSE, "Parsing PDV API response");
        TreeSet<PriceDeliveryVolumeEntity> pdvList = priceDeliveryVolumeService.parseStockData(pdvResponse, data.getTicker(), tickerContext);
        pipelineAuditService.logStageSuccess(tickerContext, PipelineAuditStage.PARSE, pdvList.size(), null, null,
                System.currentTimeMillis() - startedAt, "PDV records parsed");

        if (!pdvList.isEmpty()) {
            Optional<TradeInfo> tradeInfoOpt = Optional.empty();

            try {
                String tradeInfoUrl = fetchNSEAPI.buildTradeInfoUrl(data.getTicker());
                JsonNode tradeInfoResponse = fetchNSEAPI.NSEAPICall(tradeInfoUrl);
                tradeInfoOpt = tradeInfoService.parseTradeInfo(tradeInfoResponse, data.getTicker(), LocalDate.now(), tickerContext);
            } catch (Exception e) {
                pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.TRADEINFO_SAVE,
                        "Trade info fetch failed", e);
            }

            pdvPersistenceService.persistAll(data.getTicker(), pdvList, tradeInfoOpt, pdvResponse, tickerContext);
        }

        pipelineAuditService.logTickerSummary(tickerContext, System.currentTimeMillis() - startedAt);
    }

    private String calculateToDate(String fromDate) {
        LocalDate from = LocalDate.parse(fromDate, FORMATTER);
        LocalDate today = LocalDate.now();
        return from.plusMonths(3).isBefore(today) ? from.plusMonths(3).format(FORMATTER) : today.format(FORMATTER);
    }

    private void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class WorkItem {
        private final DataFetchEntity data;
        private final JsonNode response;

        WorkItem(DataFetchEntity data, JsonNode response) {
            this.data = data;
            this.response = response;
        }
    }

    private void fetchWithRetryAndQueue(DataFetchEntity data, BlockingQueue<WorkItem> queue, PipelineRunContext runContext) {
        String ticker = data.getTicker();
        PipelineTickerContext tickerContext = pipelineAuditService.startTickerContext(runContext.getRunId(), ticker);

        LocalDate fromDate = data.getPdvtLastDate();
        LocalDate lastAttemptedToDate = fromDate;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String from = fromDate.format(FORMATTER);
                String to = calculateToDate(from);
                lastAttemptedToDate = LocalDate.parse(to, FORMATTER);

                long startedAt = System.currentTimeMillis();
                pipelineAuditService.logStageStart(tickerContext, PipelineAuditStage.API_FETCH, "Fetching PDV API data");

                JsonNode response = fetchNSEAPI.NSEAPICall(fetchNSEAPI.buildPDVUrl(from, to, ticker));
                pipelineAuditService.logPayload(ticker, response.toString(), PipelineAuditStage.API_FETCH);

                TreeSet<PriceDeliveryVolumeEntity> pdvList = priceDeliveryVolumeService.parseStockData(response, ticker, tickerContext);
                if (!pdvList.isEmpty()) {
                    queue.put(new WorkItem(data, response));
                    pipelineAuditService.logStageSuccess(tickerContext, PipelineAuditStage.API_FETCH, pdvList.size(), from, to,
                            System.currentTimeMillis() - startedAt, "PDV API records fetched");
                    return;
                }

                log.warn("Retry {} empty for ticker {}", attempt, ticker);
                pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.API_FETCH,
                        "No PDV records returned, retrying", new IllegalStateException("empty response"));
                fromDate = lastAttemptedToDate;

            } catch (Exception e) {
                log.error("Retry {} failed for ticker {}", attempt, ticker, e);
                pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.API_FETCH,
                        "API fetch failed", e);
            }
        }

        log.warn("No data after retries for ticker {} -> updating DFHT", ticker);
        updateDfhtAfterFailure(data, lastAttemptedToDate, tickerContext);
    }

    private void updateDfhtAfterFailure(DataFetchEntity data, LocalDate lastAttemptedToDate, PipelineTickerContext tickerContext) {
        try {
            pipelineAuditService.logStageStart(tickerContext, PipelineAuditStage.DFHT_UPDATE, "Updating DFHT after API failure");
            dataFetchHistoryService.updateLastDateForPdvt(data.getTicker(), lastAttemptedToDate, tickerContext);
            pipelineAuditService.logStageSuccess(tickerContext, PipelineAuditStage.DFHT_UPDATE, 1, null, null,
                    null, "DFHT updated after API failure");
        } catch (Exception e) {
            pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.DFHT_UPDATE,
                    "Failed to update DFHT for ticker", e);
        }
    }
}