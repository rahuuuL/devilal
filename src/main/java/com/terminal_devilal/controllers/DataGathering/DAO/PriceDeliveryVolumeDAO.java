package com.terminal_devilal.controllers.DataGathering.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolumeId;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceDeliveryVolumeDAO extends JpaRepository<PriceDeliveryVolume, PriceDeliveryVolumeId> {

	List<PriceDeliveryVolume> findByTickerAndDateBetween(String ticker, LocalDate startDate, LocalDate endDate);
}

