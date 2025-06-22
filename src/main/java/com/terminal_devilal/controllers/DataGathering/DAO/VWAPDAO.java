package com.terminal_devilal.controllers.DataGathering.DAO;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.controllers.DataGathering.Model.TickerDateId;
import com.terminal_devilal.controllers.DataGathering.Model.VWAP;

@Repository
public interface VWAPDAO extends JpaRepository<VWAP, TickerDateId> {

	List<VWAP> findByDateGreaterThanEqualOrderByDateAsc(LocalDate date);

	List<VWAP> findByTickerAndDateGreaterThanEqualOrderByDateAsc(String ticker, LocalDate date);

}
