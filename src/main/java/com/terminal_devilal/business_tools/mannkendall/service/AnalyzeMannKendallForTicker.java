package com.terminal_devilal.business_tools.mannkendall.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.mannkendall.calculator.MannKendallCalcResult;
import com.terminal_devilal.business_tools.mannkendall.calculator.MannKendallCalculator;
import com.terminal_devilal.business_tools.mannkendall.calculator.MannKendallCalculatorImpl;
import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallAPIResponse;
import com.terminal_devilal.indicators.common_entities.TickerValue;
import com.terminal_devilal.indicators.pdv.repository.PriceDeliveryVolumeRepositoryCustomImpl;
import com.terminal_devilal.utils.python_server_service.PythonStatsServerAPIService;

@Service
public class AnalyzeMannKendallForTicker {

	private final PriceDeliveryVolumeRepositoryCustomImpl customImpl;
	private final MannKendallCalculator calculator;
	private static final Logger log = LoggerFactory.getLogger(AnalyzeMannKendallForTicker.class);
	long start = 0;

	public AnalyzeMannKendallForTicker(PriceDeliveryVolumeRepositoryCustomImpl customImpl,
			PythonStatsServerAPIService pythonClient) {
		super();
		this.customImpl = customImpl;
		this.calculator = new MannKendallCalculatorImpl();
	}

	public List<MannKendallAPIResponse> getMannKendallTrendAnalysis(LocalDate fromDate, LocalDate toDate) {
		start = System.currentTimeMillis();
		List<TickerValue> groupedClosePrices = customImpl.fetchTickerValuesByColumn(fromDate, toDate, "close");
		log.info("Data fetch Time = {} ms", System.currentTimeMillis() - start);

		return analysisProcessWithCalculator(groupedClosePrices);
	}

	private List<MannKendallAPIResponse> analysisProcessWithCalculator(List<TickerValue> groupedClosePrices) {
		Map<String, List<Double>> tickerMap = new HashMap<>();

		for (TickerValue tv : groupedClosePrices) {
			tickerMap.computeIfAbsent(tv.getTicker(), k -> new ArrayList<>()).add(Math.log(tv.getValue()));
		}

		List<MannKendallAPIResponse> batchResponse = new ArrayList<>();
		for (Map.Entry<String, List<Double>> entry : tickerMap.entrySet()) {
			try {
				MannKendallCalcResult result = calculator.calculate(entry.getValue());
				MannKendallAPIResponse response = new MannKendallAPIResponse();
				response.setTicker(entry.getKey());
				response.setTrend(result.getTrend());
				response.setH(result.isH());
				response.setP(result.getP());
				response.setZ(result.getZ());
				response.setTau(result.getTau());
				response.setS((double) result.getS());
				response.setVar_s(result.getVarS());
				response.setSlope(result.getSlope());
				response.setIntercept(result.getIntercept());

				double score = result.getSlope() * Math.abs(result.getTau())
						* (result.getZ() / (1.0 + Math.abs(result.getZ())));
				response.setScore(score);
				batchResponse.add(response);
			} catch (IllegalArgumentException ex) {
				log.warn("Skipping ticker {} for Mann-Kendall v2 analysis: {}", entry.getKey(), ex.getMessage());
			}
		}

		return batchResponse.stream().filter(
				resp -> resp.getP() != null && resp.getZ() != null && resp.getS() != null && resp.getVar_s() != null)
				.sorted(Comparator.comparing(MannKendallAPIResponse::getScore,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

}
