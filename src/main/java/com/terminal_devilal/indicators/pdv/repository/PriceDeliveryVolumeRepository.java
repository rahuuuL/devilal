package com.terminal_devilal.indicators.pdv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.indicators.common_entities.TickerDateId;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.entities.StockClosePrice;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceDeliveryVolumeRepository extends JpaRepository<PriceDeliveryVolumeEntity, TickerDateId> {

	List<PriceDeliveryVolumeEntity> findByTickerAndDateBetween(String ticker, LocalDate startDate, LocalDate endDate);

	@Query("SELECT sp.ticker AS ticker, sp.close AS close "
			+ "FROM PriceDeliveryVolumeEntity sp WHERE sp.date >= :from ORDER BY sp.date")
	List<StockClosePrice> getClosePrices(@Param("from") LocalDate from);

	@Query("SELECT sp.ticker AS ticker, sp.close AS close "
			+ "FROM PriceDeliveryVolumeEntity sp WHERE sp.date >= :from AND sp.ticker IN (:tickers) ORDER BY sp.date")
	List<StockClosePrice> getClosePricesForStocks(@Param("from") LocalDate from, @Param("tickers") List<String> tickers);
	
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

}
