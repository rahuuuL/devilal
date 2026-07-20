package com.terminal_devilal.indicators.pdv.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.entities.StockClosePrice;
import com.terminal_devilal.indicators.pdv.entities.projections.ClosePriceProjection;
import com.terminal_devilal.indicators.pdv.entities.projections.ConsistentVolumeProjection;
import com.terminal_devilal.indicators.pdv.entities.projections.PriceOhlcvProjection;
import com.terminal_devilal.indicators.pdv.repository.PriceDeliveryVolumeRepository;
import com.terminal_devilal.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.pipeline.audit.PipelineTickerContext;

@Service
public class PriceDeliveryVolumeService {

    private static final Logger log = LoggerFactory.getLogger(PriceDeliveryVolumeService.class);

    private final PriceDeliveryVolumeRepository repository;
    private final PriceDeliveryVolumeUtility utils;
    private final PipelineAuditService pipelineAuditService;

    public PriceDeliveryVolumeService(PriceDeliveryVolumeRepository repository, PriceDeliveryVolumeUtility utils,
            PipelineAuditService pipelineAuditService) {
        this.repository = repository;
        this.utils = utils;
        this.pipelineAuditService = pipelineAuditService;
    }

    @Transactional
    public void savePdv(PriceDeliveryVolumeEntity data) {
        repository.save(data);
    }

    public void saveAllPdvList(List<PriceDeliveryVolumeEntity> dataList) {
        repository.saveAll(dataList);
    }

    public void saveAllPdvList(List<PriceDeliveryVolumeEntity> dataList, PipelineTickerContext tickerContext) {
        repository.saveAll(dataList);
        if (tickerContext != null) {
            tickerContext.getMetrics().incrementPdvtSaved();
            pipelineAuditService.logStageSuccess(tickerContext, PipelineAuditStage.PDVT_SAVE, dataList.size(), null, null, null, "PDVT records persisted");
        }
    }

    public List<PriceOhlcvProjection> getAllPdvWithinDate(List<String> tickers, LocalDate fromDate, LocalDate toDate) {
        return repository.findByTickerInAndDateBetween(tickers, fromDate, toDate);
    }

    public List<PriceOhlcvProjection> getLatestRecordForTickers(List<String> tickers) {
        return repository.findLatestRecordForTickers(tickers);
    }

    public Map<String, List<Double>> getGroupedClosePrices(LocalDate fromDate) {
        return repository.getClosePrices(fromDate).stream()
                .collect(Collectors.groupingBy(StockClosePrice::getTicker, Collectors.mapping(StockClosePrice::getClose, Collectors.toList())));
    }

    public Map<String, List<Double>> getClosePricesForTickerSince(LocalDate fromDate, List<String> tickers) {
        return repository.getClosePricesForStocks(fromDate, tickers).stream().collect(Collectors.groupingBy(
                StockClosePrice::getTicker, Collectors.mapping(StockClosePrice::getClose, Collectors.toList())));
    }

    public Map<String, List<PriceDeliveryVolumeEntity>> getPDVForTickerSince(LocalDate fromDate, List<String> tickers) {
        return repository.getPDVForTickers(fromDate, tickers).stream()
                .collect(Collectors.groupingBy(PriceDeliveryVolumeEntity::getTicker));
    }

    public TreeSet<PriceDeliveryVolumeEntity> parseStockData(JsonNode node, String ticker) {
        return parseStockData(node, ticker, null);
    }

    public TreeSet<PriceDeliveryVolumeEntity> parseStockData(JsonNode node, String ticker, PipelineTickerContext tickerContext) {
        TreeSet<PriceDeliveryVolumeEntity> stockList = new TreeSet<>(Comparator.comparing(PriceDeliveryVolumeEntity::getDate));
        JsonNode dataArray = node.path("data");

        if (!dataArray.isArray()) {
            log.warn("PDV payload had no data array for ticker {}", ticker);
            if (tickerContext != null) {
                pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.PARSE,
                        "PDV payload had no data array", new IllegalStateException("No data array"));
            }
            return stockList;
        }

        for (JsonNode item : dataArray) {
	        PriceDeliveryVolumeEntity stock = utils.parseStockData(item, ticker);
            stockList.add(stock);
            if (tickerContext != null) {
                tickerContext.getMetrics().incrementParsed();
            }
        }
        if (tickerContext != null) {
            pipelineAuditService.logStageSuccess(tickerContext, PipelineAuditStage.PARSE, stockList.size(), null, null, null, "PDV records parsed");
        }
        return stockList;
    }

    public List<ClosePriceProjection> ClosePricesWithBufferInDateRangeForTickers(LocalDate fromDate, LocalDate toDate,
            List<String> tickers, int window) {
        return repository.getAllCloseBetweenTwoDatesForTickers(tickers, fromDate.minusDays(window * 3), toDate);
    }

    public List<ConsistentVolumeProjection> getAllVolumesBetweenTwoDates(LocalDate fromDate, LocalDate toDate) {
        return repository.getAllVolumesBetweenTwoDates(fromDate, toDate);
    }

    public List<ClosePriceProjection> getAllClosesBetweenTwoDates(LocalDate fromDate, LocalDate toDate) {
        return repository.getAllCloseBetweenTwoDates(fromDate, toDate);
    }
}
