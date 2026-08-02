package com.terminal_devilal.indicators.pdv.cache;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.terminal_devilal.indicators.common_entities.TickerValue;
import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.entity.StockClosePrice;
import com.terminal_devilal.indicators.pdv.entity.projections.ClosePriceProjection;
import com.terminal_devilal.indicators.pdv.entity.projections.ConsistentVolumeProjection;
import com.terminal_devilal.indicators.pdv.entity.projections.PriceOhlcvProjection;

public interface PDVCacheService {

    List<PriceDeliveryVolumeEntity> findByTickerOrderByDateAsc(String ticker);

    List<PriceDeliveryVolumeEntity> findByTickerAndDateBetweenOrderByDateAsc(String ticker, LocalDate fromDate,
            LocalDate toDate);

    List<PriceDeliveryVolumeEntity> findByTickerAndDateGreaterThanEqualOrderByDateAsc(String ticker, LocalDate fromDate);

    List<PriceDeliveryVolumeEntity> findByDate(LocalDate date);

    List<PriceDeliveryVolumeEntity> findAll();

    List<String> findDistinctTicker();

    List<PriceOhlcvProjection> findByTickerInAndDateBetween(List<String> tickers, LocalDate fromDate, LocalDate toDate);

    List<StockClosePrice> getClosePrices(LocalDate fromDate);

    List<StockClosePrice> getClosePricesForStocks(LocalDate fromDate, List<String> tickers);

    List<TickerValue> getTickerValues(LocalDate fromDate, LocalDate toDate, String columnName);

    List<TickerValue> getTickerValues(LocalDate fromDate, LocalDate toDate, String columnName, List<String> tickers);

    List<TickerValue> fetchTickerValuesByColumn(LocalDate fromDate, LocalDate toDate, String inputColumnName);

    Map<String, List<Double>> fetchTickerValuesByColumn(LocalDate fromDate, LocalDate toDate, String inputColumnName,
            List<String> tickers);

    List<PriceOhlcvProjection> findLatestRecordForTickers(List<String> tickers);

    Map<String, List<PriceDeliveryVolumeEntity>> getPDVForTickers(LocalDate fromDate, List<String> tickers);

    List<ClosePriceProjection> getAllCloseBetweenTwoDates(LocalDate fromDate, LocalDate toDate);

    List<ClosePriceProjection> getAllCloseBetweenTwoDatesForTickers(List<String> tickers, LocalDate fromDate,
            LocalDate toDate);

    List<ConsistentVolumeProjection> getAllVolumesBetweenTwoDates(LocalDate fromDate, LocalDate toDate);

    void reloadCache();

    void clearCache();

    boolean clearChronicleSnapshot();

    boolean reloadFromChronicleSnapshot();

    void persistSnapshot();

    Map<String, Object> getCacheStats();
}
