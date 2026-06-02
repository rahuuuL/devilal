package com.terminal_devilal.business_tools.mannkendall.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallAPIResponse;
import com.terminal_devilal.indicators.common_entities.TickerValue;
import com.terminal_devilal.indicators.pdv.repository.PriceDeliveryVolumeRepositoryCustomImpl;
import com.terminal_devilal.utils.python_server_service.PythonStatsServerAPIService;

@Service
public class AnalyzeMannKendallForTicker {

	private final PriceDeliveryVolumeRepositoryCustomImpl customImpl;
	private final PythonStatsServerAPIService pythonClient;

	public AnalyzeMannKendallForTicker(PriceDeliveryVolumeRepositoryCustomImpl customImpl,
			PythonStatsServerAPIService pythonClient) {
		super();
		this.customImpl = customImpl;
		this.pythonClient = pythonClient;
	}

	/**
	 * Public method to get Mann-Kendall trend analysis results by ticker.
	 */
	public List<MannKendallAPIResponse> getMannKendallTrendAnalysis(LocalDate fromDate, LocalDate toDate) {
		// get all close prices
		List<TickerValue> groupedClosePrices = customImpl.fetchTickerValuesByColumn(fromDate, toDate, "close");

		return analysisProcess(groupedClosePrices, fromDate, toDate);
	}

	/**
	 * Public method to get Mann-Kendall trend analysis results by ticker.
	 */
	private List<MannKendallAPIResponse> analysisProcess(List<TickerValue> groupedClosePrices, LocalDate fromDate,
			LocalDate toDate) {

		// 1. Build Map in single pass (with log applied)
		Map<String, List<Double>> tickerMap = new HashMap<>();

		for (TickerValue tv : groupedClosePrices) {

			tickerMap.computeIfAbsent(tv.getTicker(), k -> new ArrayList<>()).add(Math.log(tv.getValue()));
		}

		// 2. Call API
		List<MannKendallAPIResponse> batchResponse = pythonClient.analyzeBatch(tickerMap);

		// 3. Filter nulls
		batchResponse = batchResponse.stream().filter(
				resp -> resp.getP() != null && resp.getZ() != null && resp.getS() != null && resp.getVar_s() != null)
				.peek(resp -> {

					double slope = resp.getSlope() != null ? resp.getSlope() : 0.0;
					double tau = resp.getTau() != null ? resp.getTau() : 0.0;
					double z = resp.getZ() != null ? resp.getZ() : 0.0;

					double score = slope * Math.abs(tau) * (z / (1.0 + Math.abs(z)));

					resp.setScore(score);
				}).sorted(Comparator.comparing(MannKendallAPIResponse::getScore,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();

		return batchResponse;
	}

}
