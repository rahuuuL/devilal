package com.terminal_devilal.indicators.pdv.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.indicators.common_entities.TickerDateId;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.entities.StockClosePrice;

@Repository
public interface PriceDeliveryVolumeRepository extends JpaRepository<PriceDeliveryVolumeEntity, TickerDateId> {

	List<PriceDeliveryVolumeEntity> findByTickerAndDateBetween(String ticker, LocalDate startDate, LocalDate endDate);

	@Query("SELECT sp.ticker AS ticker, sp.close AS close "
			+ "FROM PriceDeliveryVolumeEntity sp WHERE sp.date >= :from ORDER BY sp.date")
	List<StockClosePrice> getClosePrices(@Param("from") LocalDate from);

	@Query("SELECT sp.ticker AS ticker, sp.close AS close "
			+ "FROM PriceDeliveryVolumeEntity sp WHERE sp.date >= :from AND sp.ticker IN (:tickers) ORDER BY sp.date")
	List<StockClosePrice> getClosePricesForStocks(@Param("from") LocalDate from,
			@Param("tickers") List<String> tickers);

	List<PriceDeliveryVolumeEntity> findByTickerAndDateAfterOrderByDateAsc(String ticker, LocalDate fromDate);

	@Query("SELECT DISTINCT p.ticker FROM PriceDeliveryVolumeEntity p")
	List<String> findDistinctTicker();

	List<PriceDeliveryVolumeEntity> findByTickerAndDateGreaterThanEqualOrderByDateAsc(String ticker, LocalDate date);

	@Query(value = """
			SELECT p.*
			FROM pdvt p
			JOIN (
			    SELECT ticker, MAX(date) AS max_date
			    FROM pdvt
			    WHERE ticker IN (:tickers)
			    GROUP BY ticker
			) latest ON p.ticker = latest.ticker AND p.date = latest.max_date
			""", nativeQuery = true)
	List<PriceDeliveryVolumeEntity> findLatestRecordForTickers(@Param("tickers") List<String> tickers);

	@Query("SELECT sp "
			+ "FROM PriceDeliveryVolumeEntity sp WHERE sp.date >= :from AND sp.ticker IN (:tickers) ORDER BY sp.date")
	List<PriceDeliveryVolumeEntity> getPDVForTickers(@Param("from") LocalDate from,
			@Param("tickers") List<String> tickers);

	@Query(value = """
			SELECT
			    ticker,
			    date,
			    high,
			    low,
			    open,
			    close,
			    ltp,
			    prev_close,
			    volume,
			    value,
			    trades,
			    del_trade,
			    del_percent,
			    vwap
			FROM (
			    -- Previous :window rows
			    SELECT
			        pdvt.ticker,
			        pdvt.date,
			        pdvt.high,
			        pdvt.low,
			        pdvt.open,
			        pdvt.close,
			        pdvt.ltp,
			        pdvt.prev_close,
			        pdvt.volume,
			        pdvt.value,
			        pdvt.trades,
			        pdvt.del_trade,
			        pdvt.del_percent,
			        pdvt.vwap
			    FROM (
			        SELECT
			            pdvt.*,
			            ROW_NUMBER() OVER (ORDER BY date DESC) AS rn
			        FROM pdvt
			        WHERE ticker = :ticker
			          AND date < :fromDate
			    ) pdvt
			    WHERE rn <= :window

			    UNION ALL

			    -- Main date range
			    SELECT
			        ticker,
			        date,
			        high,
			        low,
			        open,
			        close,
			        ltp,
			        prev_close,
			        volume,
			        value,
			        trades,
			        del_trade,
			        del_percent,
			        vwap
			    FROM pdvt
			    WHERE ticker = :ticker
			      AND date >= :fromDate
			      AND date <= :toDate
			) x
			ORDER BY date
			""", nativeQuery = true)

	List<PriceDeliveryVolumeEntity> getWithWindow(@Param("ticker") String ticker, @Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate, @Param("window") int window);

}
