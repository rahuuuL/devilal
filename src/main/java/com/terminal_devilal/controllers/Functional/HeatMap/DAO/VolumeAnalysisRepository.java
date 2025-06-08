package com.terminal_devilal.controllers.Functional.HeatMap.DAO;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Model.TickerDateId;

@Repository
public interface VolumeAnalysisRepository extends JpaRepository<PriceDeliveryVolume, TickerDateId> {

	@Query(value = """
	        SELECT 
	            p.ticker,
	            p.date,
	            p.volume,
	            avg_tbl.avg_volume,
	            (p.volume / avg_tbl.avg_volume) * 100 AS volume_percent_change,
	            p.del_percent AS delivery_percentage
	        FROM pdvt p
	        JOIN (
	            SELECT ticker, AVG(volume) AS avg_volume
	            FROM pdvt
	            WHERE date BETWEEN :fromDate AND :toDate
	            GROUP BY ticker
	        ) avg_tbl ON p.ticker = avg_tbl.ticker
	        WHERE p.date BETWEEN :fromDate AND :toDate
	        GROUP by ticker
			ORDER BY  volume_percent_change DESC, delivery_percentage Desc
	        """, nativeQuery = true)
	List<Object[]> findVolumeAnalysis(@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);

}
