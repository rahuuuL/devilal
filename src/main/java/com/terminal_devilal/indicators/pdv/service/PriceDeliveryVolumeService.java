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
import com.terminal_devilal.indicators.common_entities.TickerValue;
import com.terminal_devilal.indicators.pdv.cache.PDVCacheService;
import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.entity.StockClosePrice;
import com.terminal_devilal.indicators.pdv.entity.projections.ClosePriceProjection;
import com.terminal_devilal.indicators.pdv.entity.projections.ConsistentVolumeProjection;
import com.terminal_devilal.indicators.pdv.entity.projections.PriceOhlcvProjection;
import com.terminal_devilal.indicators.pdv.repository.PriceDeliveryVolumeJdbcRepository;
import com.terminal_devilal.indicators.pdv.repository.PriceDeliveryVolumeRepository;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineTickerContext;

@Service
public class PriceDeliveryVolumeService {

    private static final Logger log = LoggerFactory.getLogger(PriceDeliveryVolumeService.class);

    private final PriceDeliveryVolumeRepository repository;
    private final PDVCacheService pdvCacheService;
    private final PriceDeliveryVolumeJdbcRepository jdbcRepository;
    private final PriceDeliveryVolumeUtility utils;
    private final PipelineAuditService pipelineAuditService;

    public PriceDeliveryVolumeService(PriceDeliveryVolumeRepository repository, PDVCacheService pdvCacheService,
            PriceDeliveryVolumeJdbcRepository jdbcRepository, PriceDeliveryVolumeUtility utils,
            PipelineAuditService pipelineAuditService) {
        this.repository = repository;
        this.pdvCacheService = pdvCacheService;
        this.jdbcRepository = jdbcRepository;
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
        return pdvCacheService.findByTickerInAndDateBetween(tickers, fromDate, toDate);
    }

    public List<PriceOhlcvProjection> getLatestRecordForTickers(List<String> tickers) {
        return pdvCacheService.findLatestRecordForTickers(tickers);
    }

    public Map<String, List<Double>> getGroupedClosePrices(LocalDate fromDate) {
        return pdvCacheService.getClosePrices(fromDate).stream()
                .collect(Collectors.groupingBy(StockClosePrice::getTicker, Collectors.mapping(StockClosePrice::getClose, Collectors.toList())));
    }

    public Map<String, List<Double>> getClosePricesForTickerSince(LocalDate fromDate, List<String> tickers) {
        return pdvCacheService.getClosePricesForStocks(fromDate, tickers).stream().collect(Collectors.groupingBy(
                StockClosePrice::getTicker, Collectors.mapping(StockClosePrice::getClose, Collectors.toList())));
    }

    public Map<String, List<PriceDeliveryVolumeEntity>> getPDVForTickerSince(LocalDate fromDate, List<String> tickers) {
        return pdvCacheService.getPDVForTickers(fromDate, tickers);
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
        return pdvCacheService.getAllCloseBetweenTwoDatesForTickers(tickers, fromDate.minusDays(window * 3), toDate);
    }

    public List<ConsistentVolumeProjection> getAllVolumesBetweenTwoDates(LocalDate fromDate, LocalDate toDate) {
        return pdvCacheService.getAllVolumesBetweenTwoDates(fromDate, toDate);
    }

    public List<ClosePriceProjection> getAllClosesBetweenTwoDates(LocalDate fromDate, LocalDate toDate) {
        return pdvCacheService.getAllCloseBetweenTwoDates(fromDate, toDate);
    }

    public List<TickerValue> fetchTickerValuesByColumn(LocalDate fromDate, LocalDate toDate, String inputColumnName) {
        List<TickerValue> fromCache = pdvCacheService.fetchTickerValuesByColumn(fromDate, toDate, inputColumnName);
        if (!fromCache.isEmpty()) {
            return fromCache;
        }
        return jdbcRepository.fetchTickerValuesByColumn(fromDate, toDate, inputColumnName);
    }

    public List<TickerValue> getTickerValues(LocalDate fromDate, LocalDate toDate, String columnName,
            List<String> tickers) {
        List<TickerValue> fromCache = pdvCacheService.getTickerValues(fromDate, toDate, columnName, tickers);
        if (!fromCache.isEmpty()) {
            return fromCache;
        }
        return jdbcRepository.fetchTickerValuesByColumn(fromDate, toDate, columnName, tickers);
    }

    public Map<String, List<Double>> fetchTickerValuesByColumn(LocalDate fromDate, LocalDate toDate,
            String inputColumnName, List<String> tickers) {
        List<TickerValue> values = getTickerValues(fromDate, toDate, inputColumnName, tickers);
        return values.stream().collect(Collectors.groupingBy(TickerValue::getTicker,
                Collectors.mapping(TickerValue::getValue, Collectors.toList())));
    }
}
