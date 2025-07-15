package com.terminal_devilal.controllers.DataGathering.DAO;

import org.springframework.data.jpa.repository.JpaRepository;

import com.terminal_devilal.controllers.DataGathering.Model.TickerDateId;
import com.terminal_devilal.controllers.DataGathering.Model.TradeInfo;

public interface TradeInfoRepository extends JpaRepository<TradeInfo, TickerDateId>  {

}
