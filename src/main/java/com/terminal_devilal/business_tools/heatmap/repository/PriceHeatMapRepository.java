package com.terminal_devilal.business_tools.heatmap.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.heatmap.entities.PriceHeatMapProjection;
import com.terminal_devilal.indicators.common_entities.TickerDateId;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;

@Repository
public interface PriceHeatMapRepository extends JpaRepository<PriceDeliveryVolumeEntity, TickerDateId> {

	@Query("SELECT s1.id.ticker AS ticker, s1.open AS open, s2.close AS close, ((s2.close - s1.open) / s1.open) * 100 AS percentChange "
			+ "FROM PriceDeliveryVolumeEntity s1 " + "JOIN PriceDeliveryVolumeEntity s2 ON s1.id.ticker = s2.id.ticker "
			+ "WHERE s1.id.date = :fromDate AND s2.id.date = :toDate")
	List<PriceHeatMapProjection> getHeatMapData(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

}
