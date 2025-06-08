package com.terminal_devilal.controllers.DataGathering.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Model.TickerDateId;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceDeliveryVolumeDAO extends JpaRepository<PriceDeliveryVolume, TickerDateId> {

	List<PriceDeliveryVolume> findByTickerAndDateBetween(String ticker, LocalDate startDate, LocalDate endDate);

	@Query("SELECT sp.ticker AS ticker, sp.close AS close "
			+ "FROM PriceDeliveryVolume sp WHERE sp.date >= :from ORDER BY sp.date")
	List<StockClosePrice> getClosePrices(@Param("from") LocalDate from);

	@Query("SELECT sp.ticker AS ticker, sp.close AS close "
			+ "FROM PriceDeliveryVolume sp WHERE sp.date >= :from AND sp.ticker IN (:tickers) ORDER BY sp.date")
	List<StockClosePrice> getClosePricesForStocks(@Param("from") LocalDate from, @Param("tickers") List<String> tickers);
}
