package com.terminal_devilal.indicators.price.service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.indicators.pdv.entities.projections.RollingPriceSlopeProjection;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.indicators.price.model.RollingSlopeResult;

@Service
public class RollingSlopeCalculator {

	private final PriceDeliveryVolumeService priceVolumeService;

	public RollingSlopeCalculator(PriceDeliveryVolumeService priceVolumeService) {
		super();
		this.priceVolumeService = priceVolumeService;
	}

	private static List<RollingSlopeResult> calculate(List<RollingPriceSlopeProjection> orderedData, int windowSize) {

		if (windowSize < 2) {
			throw new IllegalArgumentException("Window size must be >= 2");
		}

		List<CompletableFuture<List<RollingSlopeResult>>> futures = new ArrayList<>();
		List<RollingPriceSlopeProjection> currentTickerBatch = new ArrayList<>();
		String currentTicker = null;

		for (RollingPriceSlopeProjection point : orderedData) {

			String ticker = point.getTicker();

			// Ticker changed → fire off the completed batch asynchronously
			if (currentTicker != null && !ticker.equals(currentTicker)) {
				final List<RollingPriceSlopeProjection> batch = currentTickerBatch; // capture for lambda
				futures.add(CompletableFuture.supplyAsync(() -> calculateForTicker(batch, windowSize),
						ForkJoinPool.commonPool()));
				currentTickerBatch = new ArrayList<>(); // fresh batch for new ticker
			}

			currentTicker = ticker;
			currentTickerBatch.add(point);
		}

		// Don't forget the last ticker's batch
		if (!currentTickerBatch.isEmpty()) {
			final List<RollingPriceSlopeProjection> batch = currentTickerBatch;
			futures.add(CompletableFuture.supplyAsync(() -> calculateForTicker(batch, windowSize),
					ForkJoinPool.commonPool()));
		}

		// Join all futures and flatten
		return futures.stream().map(CompletableFuture::join).flatMap(List::stream).collect(Collectors.toList());
	}

	// Extracted per-ticker logic (pure, no shared state — safe for parallel
	// execution)
	private static List<RollingSlopeResult> calculateForTicker(List<RollingPriceSlopeProjection> tickerData,
			int windowSize) {

		List<RollingSlopeResult> results = new ArrayList<>();
		Deque<Double> window = new ArrayDeque<>(windowSize);

		for (RollingPriceSlopeProjection point : tickerData) {
			window.addLast(point.getPrice());

			if (window.size() == windowSize) {
				double slope = computeSlope(window);
				results.add(new RollingSlopeResult(point.getTicker(), point.getDate(), slope));
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

	public List<RollingSlopeResult> getRollingSlope(LocalDate fromDate, LocalDate toDate, int windowSize) {
		// Get all data
		List<RollingPriceSlopeProjection> prices = this.priceVolumeService.getAllPricesBetweenTwoDates(fromDate,
				toDate);
		return calculate(prices, windowSize);

	}

}
