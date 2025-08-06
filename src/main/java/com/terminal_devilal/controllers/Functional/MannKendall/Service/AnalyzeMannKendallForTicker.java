package com.terminal_devilal.controllers.Functional.MannKendall.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.terminal_devilal.Utils.PythonStatsServer.PythonStatsServerAPIService;
import com.terminal_devilal.controllers.DataGathering.DAO.PriceDeliveryVolumeRepositoryCustomImpl;
import com.terminal_devilal.controllers.DataGathering.DAO.TradeInfoRepository;
import com.terminal_devilal.controllers.DataGathering.Model.TradeInfo;
import com.terminal_devilal.controllers.Functional.MannKendall.Model.MannKendallAPIResponse;

@Service
public class AnalyzeMannKendallForTicker {

	private static final Logger log = LoggerFactory.getLogger(AnalyzeMannKendallForTicker.class);

	private final TradeInfoRepository tradeInfoDao;
	private final PriceDeliveryVolumeRepositoryCustomImpl customImpl;
	private final PythonStatsServerAPIService pythonClient;

	public AnalyzeMannKendallForTicker(TradeInfoRepository tradeInfoDao,
			PriceDeliveryVolumeRepositoryCustomImpl customImpl, PythonStatsServerAPIService pythonClient) {
		super();
		this.tradeInfoDao = tradeInfoDao;
		this.customImpl = customImpl;
		this.pythonClient = pythonClient;
	}

	/**
	 * Public method to get Mann-Kendall trend analysis results by ticker.
	 */
	public List<MannKendallAPIResponse> getMannKendallTrendAnalysis(LocalDate fromDate, String inputColumnName) {
		Map<String, List<Double>> groupedClosePrices = customImpl.fetchTickerValuesByColumn(fromDate, inputColumnName);
		return analysisProcess(groupedClosePrices);
	}

	/**
	 * Public method to get Mann-Kendall trend analysis results by ticker.
	 */
	public List<MannKendallAPIResponse> getMannKendallTrendAnalysis(LocalDate fromDate, String inputColumnName,
			List<String> tickers) {
		Map<String, List<Double>> groupedClosePrices = customImpl.fetchTickerValuesByColumn(fromDate, inputColumnName,
				tickers);
		return analysisProcess(groupedClosePrices);
	}

	private List<MannKendallAPIResponse> analysisProcess(Map<String, List<Double>> groupedClosePrices) {
		// Call batch API once with all tickers & prices
		List<MannKendallAPIResponse> batchResponse = pythonClient.analyzeBatch(groupedClosePrices);

		for (MannKendallAPIResponse resp : batchResponse) {
			TradeInfo tradeInfo = fetchLatestTradeInfo(resp.getTicker());
			resp.setTradeInfo(tradeInfo);
		}

		// Null handling
		batchResponse = batchResponse.stream().filter(
				resp -> resp.getP() != null && resp.getZ() != null && resp.getS() != null && resp.getVar_s() != null)
				.collect(Collectors.toList());

		Comparator<MannKendallAPIResponse> comparator = Comparator
				.comparing((MannKendallAPIResponse resp) -> resp.getSlope() != null ? resp.getSlope()
						: Double.NEGATIVE_INFINITY, Comparator.reverseOrder())
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getZ).reversed())
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getS).reversed())
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getVar_s))
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getTotalMarketCap).reversed());

		batchResponse.sort(comparator);

		return batchResponse;
	}

	/**
	 * Retrieve the latest TradeInfo for the given ticker. Returns an empty
	 * TradeInfo if none found.
	 */
	private TradeInfo fetchLatestTradeInfo(String ticker) {
		return tradeInfoDao.findFirstByTickerOrderByDateDesc(ticker).orElseGet(() -> {
			log.warn("No TradeInfo found for ticker {}", ticker);
			return new TradeInfo(); // or null if you want to handle null
		});
	}

}
