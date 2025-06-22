package com.terminal_devilal.controllers.DataGathering.DAO;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.controllers.DataGathering.Model.RSI;
import com.terminal_devilal.controllers.DataGathering.Model.TickerDateId;

@Repository
public interface RSIDAO extends JpaRepository<RSI, TickerDateId> {

	@Query("SELECT s FROM RSI s WHERE s.ticker = :ticker ORDER BY s.date DESC")
	List<RSI> findRecentRSIs(@Param("ticker") String ticker, Pageable pageable);

	List<RSI> findByDate(LocalDate date);

	List<RSI> findByTickerAndDateGreaterThanEqualOrderByDateAsc(String ticker, LocalDate fromDate);

}
