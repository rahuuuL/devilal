package com.terminal_devilal.indicators.atr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.indicators.atr.entities.AverageTrueRangeEntity;
import com.terminal_devilal.indicators.atr.entities.projections.TrueRangeProjection;
import com.terminal_devilal.indicators.common_entities.TickerDateId;

@Repository
public interface AverageTrueRangeRepository extends JpaRepository<AverageTrueRangeEntity, TickerDateId> {

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
