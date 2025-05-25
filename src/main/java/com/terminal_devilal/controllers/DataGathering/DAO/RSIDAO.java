package com.terminal_devilal.controllers.DataGathering.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.controllers.DataGathering.Model.RSI;
import com.terminal_devilal.controllers.DataGathering.Model.TickerDateId;

@Repository
public interface RSIDAO extends JpaRepository<RSI, TickerDateId> {

}
