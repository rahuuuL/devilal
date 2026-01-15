package com.terminal_devilal.indicators.rsi.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.indicators.common_entities.TickerDateId;
import com.terminal_devilal.indicators.rsi.entities.RSIEntity;

@Repository
public interface RSIRepository extends JpaRepository<RSIEntity, TickerDateId> {

	@Query("SELECT s FROM RSIEntity s WHERE s.ticker = :ticker AND s.date <= :date ORDER BY s.date DESC LIMIT 14")
	List<RSIEntity> findRecent14RSIs(@Param("ticker") String ticker, @Param("date") LocalDate date);

	@Query("SELECT s FROM RSIEntity s WHERE s.ticker = :ticker AND s.date <= :date ORDER BY s.date DESC LIMIT 21")
	List<RSIEntity> findRecent21RSIs(@Param("ticker") String ticker, @Param("date") LocalDate date);

	List<RSIEntity> findByDate(LocalDate date);

	List<RSIEntity> findByTickerAndDateGreaterThanEqualOrderByDateAsc(String ticker, LocalDate fromDate);

	@Query(value = """
			SELECT *
			FROM (
			    SELECT s.*,
			           ROW_NUMBER() OVER (PARTITION BY s.ticker ORDER BY s.date DESC) AS rn
			    FROM rsi s
			    WHERE s.ticker IN (:tickers)
			      AND s.date <= :cutoffDate
			) t
			WHERE t.rn <= :days
			ORDER BY t.ticker, t.date DESC
			""", nativeQuery = true)
	List<RSIEntity> trackRSIData(@Param("tickers") List<String> tickers, @Param("cutoffDate") LocalDate cutoffDate,
			@Param("days") int days);

	@Query("""
			    SELECT r
			    FROM RSIEntity r
			    WHERE r.date BETWEEN :fromDate AND :toDate
			""")
	List<RSIEntity> findAllBetweenDates(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

}
