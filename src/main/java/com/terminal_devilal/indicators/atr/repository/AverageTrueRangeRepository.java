package com.terminal_devilal.indicators.atr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.indicators.atr.entity.AverageTrueRangeEntity;
import com.terminal_devilal.indicators.atr.entity.projections.TrueRangeProjection;
import com.terminal_devilal.indicators.common_entities.TickerDateId;

@Repository
public interface AverageTrueRangeRepository extends JpaRepository<AverageTrueRangeEntity, TickerDateId> {

	@Modifying
	@Transactional
	@Query(value = """
			INSERT INTO atrt (ticker, date, true_range)
			VALUES (:ticker, :date, :trueRange)
			ON DUPLICATE KEY UPDATE
				true_range = VALUES(true_range)
			""", nativeQuery = true)
	void upsert(@Param("ticker") String ticker, @Param("date") java.time.LocalDate date,
			@Param("trueRange") double trueRange);

	@Query(value = """
			SELECT
			    t.ticker AS ticker,
			    t.date AS date,
			    t.true_range AS trueRange
			FROM (
			    SELECT
			        ticker,
			        date,
			        true_range,
			        ROW_NUMBER() OVER (
			            PARTITION BY ticker
			            ORDER BY date DESC
			        ) AS rn
			    FROM atrt
			    WHERE ticker IN (:tickers)
			) t
			WHERE t.rn <= :n
			ORDER BY t.ticker, t.date DESC
			""", nativeQuery = true)
	List<TrueRangeProjection> findLastNRecordsPerTicker(@Param("tickers") List<String> tickers, @Param("n") int n);
}
