package com.terminal_devilal.business_tools.mannkendall.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallAPIResponse;
import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeRatioDTO;
import com.terminal_devilal.business_tools.ratio_analysis.service.SharpeRatioService;
import com.terminal_devilal.business_tools.trade_info.entities.TradeInfo;
import com.terminal_devilal.business_tools.trade_info.repository.TradeInfoRepository;
import com.terminal_devilal.indicators.pdv.repository.PriceDeliveryVolumeRepositoryCustomImpl;
import com.terminal_devilal.utils.python_server_service.PythonStatsServerAPIService;

@Service
public class AnalyzeMannKendallForTicker {

	private static final Logger log = LoggerFactory.getLogger(AnalyzeMannKendallForTicker.class);

	private final TradeInfoRepository tradeInfoDao;
	private final PriceDeliveryVolumeRepositoryCustomImpl customImpl;
	private final PythonStatsServerAPIService pythonClient;

	private final SharpeRatioService ratioService;

	public AnalyzeMannKendallForTicker(TradeInfoRepository tradeInfoDao,
			PriceDeliveryVolumeRepositoryCustomImpl customImpl, PythonStatsServerAPIService pythonClient,
			SharpeRatioService ratioService) {
		super();
		this.tradeInfoDao = tradeInfoDao;
		this.customImpl = customImpl;
		this.pythonClient = pythonClient;
		this.ratioService = ratioService;
	}

	/**
	 * Public method to get Mann-Kendall trend analysis results by ticker.
	 */
	public List<MannKendallAPIResponse> getMannKendallTrendAnalysis(LocalDate fromDate, String inputColumnName,
			double riskFreeRate) {
		Map<String, List<Double>> groupedClosePrices = customImpl.fetchTickerValuesByColumn(fromDate, inputColumnName);
		return analysisProcess(groupedClosePrices, fromDate, inputColumnName);
	}

	/**
	 * Public method to get Mann-Kendall trend analysis results by ticker.
	 */
	public List<MannKendallAPIResponse> getMannKendallTrendAnalysis(LocalDate fromDate, String inputColumnName,
			List<String> tickers, double riskFreeRate) {
		Map<String, List<Double>> groupedClosePrices = customImpl.fetchTickerValuesByColumn(fromDate, inputColumnName,
				tickers);
		return analysisProcess(groupedClosePrices, fromDate, inputColumnName);
	}

	private List<MannKendallAPIResponse> analysisProcess(Map<String, List<Double>> groupedClosePrices,
			LocalDate fromDate, String inputColumnName) {

		// Call batch API once with all tickers & prices
		List<MannKendallAPIResponse> batchResponse = pythonClient.analyzeBatch(groupedClosePrices);

		Map<String, SharpeRatioDTO> ratios = new HashMap<>();

		// Get all the ratios
		if (!inputColumnName.equals("volume")) {
			ratios = this.ratioService.computeSharpeRatios(fromDate, 0.06,
					batchResponse.stream().map(f -> f.getTicker()).collect(Collectors.toList()));
		}

		// set trade info and ratios
		for (MannKendallAPIResponse resp : batchResponse) {
			TradeInfo tradeInfo = fetchLatestTradeInfo(resp.getTicker());
			resp.setTradeInfo(tradeInfo);
			if (!inputColumnName.equals("volume")) {
				resp.setSharpeRatioDTO(ratios.get(resp.getTicker()));
			}

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
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getTotalMarketCap).reversed())
				.thenComparing(resp -> {
					SharpeRatioDTO dto = resp.getSharpeRatioDTO();
					return dto != null ? dto.getRawSharpe() : Double.MIN_VALUE;
				}, Comparator.reverseOrder()).thenComparing(resp -> {
					SharpeRatioDTO dto = resp.getSharpeRatioDTO();
					return dto != null ? dto.getRawSortino() : Double.MIN_VALUE;
				}, Comparator.reverseOrder() // desc order
				);

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
