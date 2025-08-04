package com.terminal_devilal.controllers.Functional.MannKendall.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.terminal_devilal.controllers.DataGathering.DAO.TradeInfoRepository;
import com.terminal_devilal.controllers.DataGathering.Model.TradeInfo;
import com.terminal_devilal.controllers.DataGathering.Service.PriceDeliveryVolumeService;
import com.terminal_devilal.controllers.Functional.MannKendall.Model.MannKendallAPIResponse;
import com.terminal_devilal.controllers.Functional.MannKendall.Model.MannKendallResponse;

@Service
public class AnalyzeMannKendallForTicker {

	private static final Logger log = LoggerFactory.getLogger(AnalyzeMannKendallForTicker.class);

	private final PriceDeliveryVolumeService deliveryVolumeService;
	private final MannKendallService kendallService;
	private final TradeInfoRepository tradeInfoDao;

	public AnalyzeMannKendallForTicker(PriceDeliveryVolumeService deliveryVolumeService,
			MannKendallService kendallService, TradeInfoRepository tradeInfoDao) {
		this.deliveryVolumeService = deliveryVolumeService;
		this.kendallService = kendallService;
		this.tradeInfoDao = tradeInfoDao;
	}

	/**
	 * Public method to get Mann-Kendall trend analysis results by ticker.
	 */
	public List<MannKendallAPIResponse> getMannKendallTrendAnalysis(LocalDate fromDate) {
		Map<String, List<Double>> groupedClosePrices = getGroupedClosePrices(fromDate);
		return analysisProcess(groupedClosePrices);
	}

	/**
	 * Public method to get Mann-Kendall trend analysis results by ticker.
	 */
	public List<MannKendallAPIResponse> getMannKendallTrendAnalysis(LocalDate fromDate, List<String> tickers) {
		Map<String, List<Double>> groupedClosePrices = getClosePricesForTickerSince(fromDate, tickers);
		return analysisProcess(groupedClosePrices);
	}

	private List<MannKendallAPIResponse> analysisProcess(Map<String, List<Double>> groupedClosePrices) {
		List<MannKendallAPIResponse> list = groupedClosePrices.entrySet().parallelStream().map(this::processTickerEntry)
				.filter(Objects::nonNull).collect(Collectors.toList());

		list.sort(Comparator.comparingDouble(MannKendallAPIResponse::getTotalMarketCap).reversed()
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getP))
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getZ).reversed())
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getS).reversed())
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getVar_s))

		);

		return list;
	}

	/**
	 * Get grouped close prices keyed by ticker.
	 */
	private Map<String, List<Double>> getGroupedClosePrices(LocalDate fromDate) {
		return deliveryVolumeService.getGroupedClosePrices(fromDate);
	}

	/**
	 * Get grouped close prices keyed by ticker.
	 */
	private Map<String, List<Double>> getClosePricesForTickerSince(LocalDate fromDate, List<String> tickers) {
		return deliveryVolumeService.getClosePricesForTickerSince(fromDate, tickers);
	}

	/**
	 * Process a map entry of ticker to its price series. Returns
	 * MannKendallAPIResponse or null if error/empty data.
	 */
	private MannKendallAPIResponse processTickerEntry(Map.Entry<String, List<Double>> entry) {
		String ticker = entry.getKey();
		List<Double> priceSeries = entry.getValue();

		if (priceSeries == null || priceSeries.isEmpty()) {
			log.warn("Skipping ticker {} due to empty price series.", ticker);
			return null;
		}

		try {
			MannKendallResponse mkResponse = kendallService.runMannKendall(priceSeries);
			TradeInfo tradeInfo = fetchLatestTradeInfo(ticker);
			return new MannKendallAPIResponse(ticker, tradeInfo, mkResponse);
		} catch (Exception e) {
			log.error("Failed to process ticker {}: {}", ticker, e.getMessage(), e);
			return null;
		}
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
