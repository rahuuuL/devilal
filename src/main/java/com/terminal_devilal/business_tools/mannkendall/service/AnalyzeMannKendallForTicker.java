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
import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallResponse;
import com.terminal_devilal.indicators.common_entities.TickerValue;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;

@Service
public class AnalyzeMannKendallForTicker {

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;
	private final MannKendallCalculator calculator;
	private static final Logger log = LoggerFactory.getLogger(AnalyzeMannKendallForTicker.class);
	long start = 0;

	public AnalyzeMannKendallForTicker(PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
		this.calculator = new MannKendallCalculatorImpl();
	}

	public List<MannKendallResponse> getMannKendallTrendAnalysis(LocalDate fromDate, LocalDate toDate) {
		start = System.currentTimeMillis();
		List<TickerValue> groupedClosePrices = priceDeliveryVolumeService.fetchTickerValuesByColumn(fromDate, toDate,
				"close");
		log.info("Data fetch Time = {} ms", System.currentTimeMillis() - start);

		return analysisProcessWithCalculator(groupedClosePrices);
	}

	private List<MannKendallResponse> analysisProcessWithCalculator(List<TickerValue> groupedClosePrices) {
		Map<String, List<Double>> tickerMap = new HashMap<>();

		for (TickerValue tv : groupedClosePrices) {
			tickerMap.computeIfAbsent(tv.getTicker(), k -> new ArrayList<>()).add(Math.log(tv.getValue()));
		}

		return tickerMap.entrySet().parallelStream().map(entry -> {
			try {
				MannKendallCalcResult result = calculator.calculate(entry.getValue());
				MannKendallResponse response = new MannKendallResponse();
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
				response.setScore(result.getScore());
				return response;
			} catch (IllegalArgumentException ex) {
				log.warn("Skipping ticker {} for Mann-Kendall v2 analysis: {}", entry.getKey(), ex.getMessage());
				return null;
			}
		}).filter(resp -> resp != null).filter(
				resp -> resp.getP() != null && resp.getZ() != null && resp.getS() != null && resp.getVar_s() != null)
				.sorted(Comparator.comparing(MannKendallResponse::getScore,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

}
