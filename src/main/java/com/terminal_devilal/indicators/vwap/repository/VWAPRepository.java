package com.terminal_devilal.indicators.vwap.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.indicators.common_entities.TickerDateId;
import com.terminal_devilal.indicators.vwap.entity.VWAPEntity;
import com.terminal_devilal.indicators.vwap.entity.projections.VwapProjection;

@Repository
public interface VWAPRepository extends JpaRepository<VWAPEntity, TickerDateId> {

	List<VwapProjection> findByTickerInAndDateBetween(List<String> tickers, LocalDate startDate, LocalDate endDate);

	@Modifying
	@Transactional
	@Query(value = """
			INSERT INTO vwap (ticker, date, close_price, vwap, vwap_proximity)
			VALUES (:ticker, :date, :closePrice, :vwap, :vwapProximity)
			ON DUPLICATE KEY UPDATE
				close_price = VALUES(close_price),
				vwap = VALUES(vwap),
				vwap_proximity = VALUES(vwap_proximity)
			""", nativeQuery = true)
	void upsert(@Param("ticker") String ticker, @Param("date") LocalDate date,
			@Param("closePrice") double closePrice, @Param("vwap") double vwap,
			@Param("vwapProximity") double vwapProximity);

}
