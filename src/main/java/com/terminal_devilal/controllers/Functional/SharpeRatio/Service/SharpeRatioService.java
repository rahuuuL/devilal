package com.terminal_devilal.controllers.Functional.SharpeRatio.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.controllers.DataGathering.Service.PriceDeliveryVolumeService;
import com.terminal_devilal.controllers.Functional.SharpeRatio.Model.SharpeRatioDTO;

@Service
public class SharpeRatioService {

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	public SharpeRatioService(PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}

	public Map<String, SharpeRatioDTO> computeSharpeRatios(LocalDate fromDate, double riskFreeRate) {
		Map<String, List<Double>> priceMap = priceDeliveryVolumeService.getGroupedClosePrices(fromDate);

		// Compute Sharpe Ratios in parallel
		return priceMap.entrySet().parallelStream().filter(e -> e.getValue().size() > 1)
				.collect(Collectors.toMap(Map.Entry::getKey, e -> {
					return calculateSharpe(e.getValue(), riskFreeRate);
				}));
	}

	public Map<String, SharpeRatioDTO> computeSharpeRatios(LocalDate fromDate, double riskFreeRate,
			List<String> tickers) {
		Map<String, List<Double>> priceMap = priceDeliveryVolumeService.getClosePricesForTickerSince(fromDate, tickers);

		// Compute Sharpe Ratios in parallel
		return priceMap.entrySet().parallelStream().filter(e -> e.getValue().size() > 1)
				.collect(Collectors.toMap(Map.Entry::getKey, e -> {
					return calculateSharpe(e.getValue(), riskFreeRate);
				}));
	}

	public SharpeRatioDTO calculateSharpe(List<Double> prices, double riskFreeRate) {
		if (prices.size() < 2)
			return new SharpeRatioDTO(0, 0, prices.size());

		List<Double> returns = new ArrayList<>();
		for (int i = 1; i < prices.size(); i++) {
			double r = (prices.get(i) / prices.get(i - 1)) - 1;
			returns.add(r);
		}

		double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);

		double stddev = Math.sqrt(returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0));;
//		if (returns.size() > 1) {
//			double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum() / (returns.size());
//			stddev = Math.sqrt(variance);
//		}

		int days = returns.size();

		double rawSharpe = (stddev == 0) ? 0 : (mean - riskFreeRate / 252) / stddev;

		double annualSharpe = 0;
		if (days >= 20) { // avoid annualizing small periods
			double annualReturn = mean * 252;
			double annualVolatility = stddev * Math.pow(252, 0.5);
			annualSharpe = (annualVolatility == 0) ? 0 : (annualReturn - riskFreeRate) / annualVolatility;
		}

		return new SharpeRatioDTO(rawSharpe, annualSharpe, days);
	}

}
