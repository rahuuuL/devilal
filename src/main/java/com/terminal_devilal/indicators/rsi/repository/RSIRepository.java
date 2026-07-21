package com.terminal_devilal.indicators.rsi.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.indicators.common_entities.TickerDateId;
import com.terminal_devilal.indicators.rsi.entity.RSIEntity;
import com.terminal_devilal.indicators.rsi.entity.projections.RsiPercentileProjection;
import com.terminal_devilal.indicators.rsi.entity.projections.RsiProjection;

@Repository
public interface RSIRepository extends JpaRepository<RSIEntity, TickerDateId> {

	@Modifying
	@Transactional
	@Query(value = """
			INSERT INTO rsi (ticker, date, close_diff, `14_days_rsi`, `21_days_rsi`)
			VALUES (:ticker, :date, :closeDiff, :rsi14, :rsi21)
			ON DUPLICATE KEY UPDATE
				close_diff = VALUES(close_diff),
				`14_days_rsi` = VALUES(`14_days_rsi`),
				`21_days_rsi` = VALUES(`21_days_rsi`)
			""", nativeQuery = true)
	void upsert(@Param("ticker") String ticker, @Param("date") LocalDate date,
			@Param("closeDiff") double closeDiff, @Param("rsi14") double rsi14,
			@Param("rsi21") double rsi21);

	@Query("SELECT s FROM RSIEntity s WHERE s.ticker = :ticker AND s.date <= :date ORDER BY s.date DESC LIMIT 14")
	List<RSIEntity> findRecent14RSIs(@Param("ticker") String ticker, @Param("date") LocalDate date);

	@Query("SELECT s FROM RSIEntity s WHERE s.ticker = :ticker AND s.date <= :date ORDER BY s.date DESC LIMIT 21")
	List<RSIEntity> findRecent21RSIs(@Param("ticker") String ticker, @Param("date") LocalDate date);

	List<RSIEntity> findByDate(LocalDate date);

	List<RsiProjection> findByTickerInAndDateBetween(List<String> tickers, LocalDate startDate, LocalDate endDate);

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
			    SELECT
			        r.ticker AS ticker,
			        r.date AS date,
			        r.fourteenDaysRsi AS fourtheenDaysRSI,
			        r.twentyOneDaysRsi AS twentyOneDaysRsi
			    FROM RSIEntity r
			    WHERE r.date BETWEEN :fromDate AND :toDate
			""")
	List<RsiPercentileProjection> findPercentileRecordsBetweenDates(@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);

}
