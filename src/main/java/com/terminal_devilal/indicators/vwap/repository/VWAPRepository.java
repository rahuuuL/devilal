package com.terminal_devilal.indicators.vwap.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.indicators.common_entities.TickerDateId;
import com.terminal_devilal.indicators.vwap.entities.VWAPEntity;

@Repository
public interface VWAPRepository extends JpaRepository<VWAPEntity, TickerDateId> {

	List<VWAPEntity> findByDateGreaterThanEqualOrderByDateAsc(LocalDate date);

	List<VWAPEntity> findByTickerAndDateGreaterThanEqualOrderByDateAsc(String ticker, LocalDate date);

}
