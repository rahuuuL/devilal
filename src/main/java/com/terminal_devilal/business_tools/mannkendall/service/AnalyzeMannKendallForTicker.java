package com.terminal_devilal.business_tools.mannkendall.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.mannkendall.calculator.MannKendallCalcResult;
import com.terminal_devilal.business_tools.mannkendall.calculator.MannKendallCalculator;
import com.terminal_devilal.business_tools.mannkendall.calculator.MannKendallCalculatorImpl;
import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallResponse;
import com.terminal_devilal.indicators.common_entities.TickerValue;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.utils.WorkingDayDateRangeUtil;

@Service
public class AnalyzeMannKendallForTicker {

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;
	private final MannKendallCalculator calculator;
	private static final Logger log = LoggerFactory.getLogger(AnalyzeMannKendallForTicker.class);

	public AnalyzeMannKendallForTicker(PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
		this.calculator = new MannKendallCalculatorImpl();
	}

	public List<MannKendallResponse> getMannKendallTrendAnalysis(LocalDate fromDate, LocalDate toDate) {
		List<TickerValue> groupedClosePrices = priceDeliveryVolumeService.fetchTickerValuesByColumn(fromDate, toDate,
				"close");

		return analysisProcessWithCalculator(groupedClosePrices);
	}

	public WindowAnalysisBatchResult getMannKendallTrendAnalysisByDays(LocalDate toDate, List<Integer> daysList) {
		if (toDate == null) {
			throw new IllegalArgumentException("toDate must not be null");
		}
		if (daysList == null || daysList.isEmpty()) {
			return new WindowAnalysisBatchResult(Map.of(), Map.of());
		}

		Set<Integer> normalizedDays = daysList.stream().filter(day -> day != null && day > 0)
				.collect(Collectors.toCollection(HashSet::new));
		if (normalizedDays.isEmpty()) {
			return new WindowAnalysisBatchResult(Map.of(), Map.of());
		}

		Map<Integer, LocalDate> fromDatesByDays = new HashMap<>();
		for (Integer days : normalizedDays) {
			WorkingDayDateRangeUtil.DateRange range = WorkingDayDateRangeUtil.calculateDateRange(toDate, days);
			fromDatesByDays.put(days, range.getFromDate());
		}

		LocalDate earliestFromDate = fromDatesByDays.values().stream().min(LocalDate::compareTo).orElse(toDate);

		List<TickerValue> allCloseValues = priceDeliveryVolumeService.fetchTickerValuesByColumn(earliestFromDate, toDate,
				"close");

		Map<String, List<TickerValue>> valuesByTicker = allCloseValues.stream().collect(Collectors.groupingBy(
				TickerValue::getTicker,
				Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
						.sorted(Comparator.comparing(TickerValue::getDate)).collect(Collectors.toList()))));

		Map<Integer, List<MannKendallResponse>> resultsByDays = new ConcurrentHashMap<>();
		Map<Integer, String> errorsByDays = new ConcurrentHashMap<>();

		normalizedDays.parallelStream().forEach(days -> {
			try {
				LocalDate fromDate = fromDatesByDays.get(days);
				resultsByDays.put(days, analysisProcessWithCalculator(valuesByTicker, fromDate));
			} catch (Exception exception) {
				String errorMessage = exception.getMessage() == null ? exception.getClass().getSimpleName()
						: exception.getMessage();
				errorsByDays.put(days, errorMessage);
			}
		});

		return new WindowAnalysisBatchResult(resultsByDays, errorsByDays);
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

	private List<MannKendallResponse> analysisProcessWithCalculator(Map<String, List<TickerValue>> valuesByTicker,
			LocalDate fromDate) {
		return valuesByTicker.entrySet().parallelStream().map(entry -> {
			try {
				List<Double> values = entry.getValue().stream().filter(tv -> !tv.getDate().isBefore(fromDate))
						.map(tv -> Math.log(tv.getValue())).collect(Collectors.toList());
				if (values.size() < 3) {
					return null;
				}

				MannKendallCalcResult result = calculator.calculate(values);
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
				log.warn("Skipping ticker {} for Mann-Kendall window analysis: {}", entry.getKey(), ex.getMessage());
				return null;
			}
		}).filter(resp -> resp != null)
				.filter(resp -> resp.getP() != null && resp.getZ() != null && resp.getS() != null
						&& resp.getVar_s() != null)
				.sorted(Comparator.comparing(MannKendallResponse::getScore,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	public static class WindowAnalysisBatchResult {

		private final Map<Integer, List<MannKendallResponse>> resultsByDays;
		private final Map<Integer, String> errorsByDays;

		public WindowAnalysisBatchResult(Map<Integer, List<MannKendallResponse>> resultsByDays,
				Map<Integer, String> errorsByDays) {
			this.resultsByDays = resultsByDays;
			this.errorsByDays = errorsByDays;
		}

		public Map<Integer, List<MannKendallResponse>> getResultsByDays() {
			return resultsByDays;
		}

		public Map<Integer, String> getErrorsByDays() {
			return errorsByDays;
		}
	}

}
