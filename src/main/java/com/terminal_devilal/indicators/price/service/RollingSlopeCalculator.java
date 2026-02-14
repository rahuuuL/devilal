package com.terminal_devilal.indicators.price.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import com.terminal_devilal.indicators.common_entities.TickerValue;
import com.terminal_devilal.indicators.price.model.RollingSlopeResult;

public class RollingSlopeCalculator {
	public static List<RollingSlopeResult> calculate(List<TickerValue> orderedData, int windowSize) {

		if (windowSize < 2) {
			throw new IllegalArgumentException("Window size must be >= 2");
		}

		List<RollingSlopeResult> results = new ArrayList<>();
		Deque<Double> window = new ArrayDeque<>(windowSize);

		String currentTicker = null;

		for (TickerValue point : orderedData) {

			String ticker = point.getTicker();

			// Detect ticker change
			if (currentTicker == null || !ticker.equals(currentTicker)) {
				window.clear();
				currentTicker = ticker;
			}

			// Add new value
			window.addLast(point.getValue());

			// When window is full → compute slope
			if (window.size() == windowSize) {

				double slope = computeSlope(window);

				results.add(new RollingSlopeResult(ticker, point.getDate(), slope));

				// Slide window
				window.removeFirst();
			}
		}

		return results;
	}

	private static double computeSlope(Collection<Double> y) {

		int n = y.size();
		if (n < 2)
			return 0.0;

		double sumX = 0.0;
		double sumY = 0.0;
		double sumXY = 0.0;
		double sumXX = 0.0;

		int i = 0;
		for (double yi : y) {
			double xi = i++;
			sumX += xi;
			sumY += yi;
			sumXY += xi * yi;
			sumXX += xi * xi;
		}

		double denominator = n * sumXX - sumX * sumX;
		if (denominator == 0.0) {
			return 0.0;
		}

		return (n * sumXY - sumX * sumY) / denominator;
	}
}
