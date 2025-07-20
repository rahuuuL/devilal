package com.terminal_devilal.controllers.Functional.HeatMap.DAO;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Model.TickerDateId;
import com.terminal_devilal.controllers.Functional.HeatMap.Model.VolumeAnalysisResults;

@Repository
public interface VolumeAnalysisRepository extends JpaRepository<PriceDeliveryVolume, TickerDateId> {

	@Query(value = """
			SELECT *
			FROM (
			    SELECT
			        p.ticker AS ticker,
			        p.date AS date,
			        p.volume AS volume,
			        avg_tbl.avg_volume AS avgVolume,
			        (p.volume / avg_tbl.avg_volume) AS times,
			        p.del_percent AS deliveryPercentage,
			        ROW_NUMBER() OVER (
			            PARTITION BY p.ticker
			            ORDER BY (p.volume / avg_tbl.avg_volume) DESC
			        ) AS rn
			    FROM pdvt p
			    JOIN (
			        SELECT ticker, AVG(volume) AS avg_volume
			        FROM pdvt
			        WHERE date BETWEEN :fromDate AND :toDate
			        GROUP BY ticker
			    ) avg_tbl
			      ON p.ticker = avg_tbl.ticker
			    WHERE p.date BETWEEN :fromDate AND :toDate
			) ranked
			WHERE rn = 1
			ORDER BY times DESC, deliveryPercentage DESC;
					    """, nativeQuery = true)
	List<VolumeAnalysisResults> findVolumeAnalysis(@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);

}
