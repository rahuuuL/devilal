package com.terminal_devilal.controllers.DataGathering.DAO;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.controllers.DataGathering.Model.AverageTrueRange;
import com.terminal_devilal.controllers.DataGathering.Model.TickerDateId;

@Repository
public interface AverageTrueRangeDAO extends JpaRepository<AverageTrueRange, TickerDateId> {

	List<AverageTrueRange> findByTickerAndDateGreaterThanEqualOrderByDateAsc(String ticker, LocalDate date);
}
