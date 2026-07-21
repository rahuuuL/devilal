package com.terminal_devilal.indicators.price.service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Service;

import com.terminal_devilal.indicators.pdv.entity.projections.ClosePriceProjection;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.indicators.price.model.RollingSlopeResult;
import com.terminal_devilal.utils.common_calcs.PercentileCalculator;
import com.terminal_devilal.utils.common_calcs.SlopeCalculator;

@Service
public class RollingSlopeCalculator {

	private final PriceDeliveryVolumeService priceVolumeService;

	public RollingSlopeCalculator(PriceDeliveryVolumeService priceVolumeService) {
		this.priceVolumeService = priceVolumeService;
	}

	private static List<RollingSlopeResult> calculate(List<ClosePriceProjection> orderedData, int windowSize) {

		if (windowSize < 2) {
			throw new IllegalArgumentException("Window size must be >= 2");
		}

		List<RollingSlopeResult> result = new ArrayList<>();

		String currentTicker = null;
		Deque<Double> window = new ArrayDeque<>(windowSize);

		for (ClosePriceProjection point : orderedData) {

			String ticker = point.getTicker();

			if (currentTicker == null || !ticker.equals(currentTicker)) {
				currentTicker = ticker;
				window.clear();
			}

			window.addLast(point.getClose());

			if (window.size() == windowSize) {

				double slope = SlopeCalculator.computeSlope(window);

				result.add(new RollingSlopeResult(ticker, point.getDate(), slope));

				window.removeFirst();
			}
		}

		return result;
	}

	public List<RollingSlopeResult> getRollingSlope(LocalDate fromDate, LocalDate toDate, int windowSize) {

		List<ClosePriceProjection> prices = priceVolumeService.getAllClosesBetweenTwoDates(fromDate, toDate);

		return calculate(prices, windowSize);
	}

	public List<RollingSlopeResult> computeSlopePercentiles(List<RollingSlopeResult> slopes) {

		int start = 0;

		while (start < slopes.size()) {

			String ticker = slopes.get(start).getTicker();

			int end = start;

			while (end < slopes.size() && ticker.equals(slopes.get(end).getTicker())) {
				end++;
			}

			int n = end - start;

			System.out.println("\n==============================");
			System.out.println("Ticker: " + ticker);
			System.out.println("Count : " + n);

			if (n == 0) {
				start = end;
				continue;
			}

			double[] sorted = new double[n];

			for (int i = 0; i < n; i++) {
				sorted[i] = slopes.get(start + i).getSlope();
			}

			Arrays.sort(sorted);

			System.out.println("Min slope: " + sorted[0]);
			System.out.println("Max slope: " + sorted[n - 1]);

			// percentile calculation
			for (int i = start; i < end; i++) {

				RollingSlopeResult r = slopes.get(i);

				double percentile = PercentileCalculator.computePercentileSorted(sorted, r.getSlope());

				r.setPercentile(percentile);

				System.out.println(
						"Date=" + r.getWindowEndDate() + " slope=" + r.getSlope() + " percentile=" + percentile);
			}

			start = end;
		}

		return slopes;
	}
}