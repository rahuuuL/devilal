package com.terminal_devilal.business_tools.mannkendall.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallAPIResponse;
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
	public List<MannKendallAPIResponse> getMannKendallTrendAnalysis(LocalDate fromDate, LocalDate toDate,
			String inputColumnName) {
		Map<String, List<Double>> groupedClosePrices = customImpl.fetchTickerValuesByColumn(fromDate, toDate,
				inputColumnName);
		return analysisProcess(groupedClosePrices, fromDate, toDate, inputColumnName);
	}

	/**
	 * Public method to get Mann-Kendall trend analysis results by ticker.
	 */
	public List<MannKendallAPIResponse> getMannKendallTrendAnalysis(LocalDate fromDate, LocalDate toDate,
			String inputColumnName, List<String> tickers) {
		Map<String, List<Double>> groupedClosePrices = customImpl.fetchTickerValuesByColumn(fromDate, toDate,
				inputColumnName, tickers);
		return analysisProcess(groupedClosePrices, fromDate, toDate, inputColumnName);
	}

	private List<MannKendallAPIResponse> analysisProcess(Map<String, List<Double>> groupedClosePrices,
			LocalDate fromDate, LocalDate toDate, String inputColumnName) {

		// Call batch API once with all tickers & prices
		List<MannKendallAPIResponse> batchResponse = pythonClient.analyzeBatch(groupedClosePrices);

		// Null handling
		batchResponse = batchResponse.stream().filter(
				resp -> resp.getP() != null && resp.getZ() != null && resp.getS() != null && resp.getVar_s() != null)
				.collect(Collectors.toList());

		Comparator<MannKendallAPIResponse> comparator = Comparator
				.comparing((MannKendallAPIResponse resp) -> resp.getSlope() != null ? resp.getSlope()
						: Double.NEGATIVE_INFINITY, Comparator.reverseOrder())
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getZ).reversed())
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getS).reversed())
				.thenComparing(Comparator.comparingDouble(MannKendallAPIResponse::getVar_s));

		batchResponse.sort(comparator);

		return batchResponse;
	}

}
