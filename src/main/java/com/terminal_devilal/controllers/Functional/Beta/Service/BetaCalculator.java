package com.terminal_devilal.controllers.Functional.Beta.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.terminal_devilal.controllers.DataGathering.Service.PriceDeliveryVolumeService;
import com.terminal_devilal.controllers.Functional.Beta.Exception.BetaCalcException;

@Service
public class BetaCalculator {
	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	public BetaCalculator(PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}

	public double calculateBeta(String tickerA, String tickerB, LocalDate Date) {

		Map<String, List<Double>> prices = this.priceDeliveryVolumeService.getClosePricesForTickerSince(Date,
				List.of(tickerA, tickerB));

		List<Double> pricesA = prices.get(tickerA);
		List<Double> pricesB = prices.get(tickerB);

		if (pricesA.size() != pricesB.size() || pricesA.size() < 2) {
			throw new BetaCalcException("Price lists must be of equal size and at least 2 in length");
		}

		List<Double> returnsA = new ArrayList<>();
		List<Double> returnsB = new ArrayList<>();
		
		System.out.println(pricesA.size());
		System.out.println(pricesA.size());

		for (int i = 1; i < pricesA.size(); i++) {
			returnsA.add((pricesA.get(i) / pricesA.get(i - 1)) - 1);
			returnsB.add((pricesB.get(i) / pricesB.get(i - 1)) - 1);
		}

		double meanA = returnsA.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
		double meanB = returnsB.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

		double covariance = 0.0;
		double varianceB = 0.0;

		for (int i = 0; i < returnsA.size(); i++) {
			double a = returnsA.get(i) - meanA;
			double b = returnsB.get(i) - meanB;
			covariance += a * b;
			varianceB += b * b;
		}

		covariance /= (returnsA.size() - 1); // sample covariance
		varianceB /= (returnsB.size() - 1); // sample variance

		return (varianceB == 0) ? 0 : (covariance / varianceB);
	}
}
