package com.terminal_devilal.controllers.Functional.HeatMap.DAO;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Model.TickerDateId;
import com.terminal_devilal.controllers.Functional.HeatMap.Model.HeatMapProjection;

@Repository
public interface HeatMapRepository extends JpaRepository<PriceDeliveryVolume, TickerDateId> {

	@Query("SELECT s1.id.ticker AS ticker, s1.open AS open, s2.close AS close, ((s2.close - s1.open) / s1.open) * 100 AS percentChange "
			+ "FROM PriceDeliveryVolume s1 " + "JOIN PriceDeliveryVolume s2 ON s1.id.ticker = s2.id.ticker "
			+ "WHERE s1.id.date = :fromDate AND s2.id.date = :toDate")
	List<HeatMapProjection> getHeatMapData(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

}
