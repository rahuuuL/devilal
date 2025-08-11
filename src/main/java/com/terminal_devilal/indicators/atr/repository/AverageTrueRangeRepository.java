package com.terminal_devilal.indicators.atr.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.indicators.atr.entities.AverageTrueRangeEntity;
import com.terminal_devilal.indicators.common_entities.TickerDateId;

@Repository
public interface AverageTrueRangeRepository extends JpaRepository<AverageTrueRangeEntity, TickerDateId> {

	List<AverageTrueRangeEntity> findByTickerAndDateGreaterThanEqualOrderByDateAsc(String ticker, LocalDate date);
}
