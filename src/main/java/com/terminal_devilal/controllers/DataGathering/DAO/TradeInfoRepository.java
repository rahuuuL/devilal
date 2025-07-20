package com.terminal_devilal.controllers.DataGathering.DAO;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.terminal_devilal.controllers.DataGathering.Model.TickerDateId;
import com.terminal_devilal.controllers.DataGathering.Model.TradeInfo;

public interface TradeInfoRepository extends JpaRepository<TradeInfo, TickerDateId> {

	Optional<TradeInfo> findFirstByTickerOrderByDateDesc(String ticker);

	@Query(value = """
		    SELECT ti.*
		    FROM trade_info ti
		    JOIN (
		        SELECT ticker, MAX(date) AS max_date
		        FROM trade_info
		        GROUP BY ticker
		    ) latest
		    ON ti.ticker = latest.ticker AND ti.date = latest.max_date
		    """, nativeQuery = true)
	List<TradeInfo> findLatestTradeInfoPerTicker();

}
