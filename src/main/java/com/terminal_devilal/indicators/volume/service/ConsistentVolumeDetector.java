package com.terminal_devilal.indicators.volume.service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.indicators.pdv.entities.projections.ConsistentVolumeProjection;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.indicators.volume.model.ConsistentVolumeSignalResponse;
import com.terminal_devilal.indicators.volume.utils.SortedWindow;

@Service
public class ConsistentVolumeDetector {

	private final PriceDeliveryVolumeService priceVolume;

	public ConsistentVolumeDetector(PriceDeliveryVolumeService priceVolume) {
		super();
		this.priceVolume = priceVolume;
	}

	public List<ConsistentVolumeSignalResponse> detectConsistentVolumes(LocalDate fromDate, LocalDate toDate,
			int inputBaselineWindow, double baselineLowPercentile, double baselineHighPercentile,
			int baseRvolPercentileWindow, double rvolThresholdPercentile, int consistencyWindow, int requiredScore) {

		// -------- Validation --------
		int baselineWindow = Math.max(30, inputBaselineWindow);
		int rvolPercentileWindow = Math.max(inputBaselineWindow, baseRvolPercentileWindow);

		// -------- Fetch data --------
		List<ConsistentVolumeProjection> allData = priceVolume.getAllVolumesBetweenTwoDates(fromDate, toDate);

		// -------- Output --------
		Queue<ConsistentVolumeSignalResponse> signals = new ConcurrentLinkedQueue<>();

		// -------- Thread pool --------
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		// -------- Ticker-wise grouping --------
		String previousTicker = "";
		List<ConsistentVolumeProjection> processData = new ArrayList<>();

		for (ConsistentVolumeProjection data : allData) {

			if (!previousTicker.isEmpty() && !previousTicker.equals(data.getTicker())) {

				// snapshot list (VERY important)
				List<ConsistentVolumeProjection> bars = new ArrayList<>(processData);

				futures.add(CompletableFuture.runAsync(
						() -> processData(signals, bars, baselineWindow, baselineLowPercentile, baselineHighPercentile,
								rvolPercentileWindow, rvolThresholdPercentile, consistencyWindow, requiredScore),
						executor));

				processData.clear();
			}

			previousTicker = data.getTicker();
			processData.add(data);
		}

		// -------- Last ticker --------
		if (!processData.isEmpty()) {

			List<ConsistentVolumeProjection> bars = new ArrayList<>(processData);

			futures.add(CompletableFuture.runAsync(
					() -> processData(signals, bars, baselineWindow, baselineLowPercentile, baselineHighPercentile,
							rvolPercentileWindow, rvolThresholdPercentile, consistencyWindow, requiredScore),
					executor));
		}

		// -------- Wait for all tickers --------
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		executor.shutdown();

		// -------- Sort results --------
		List<ConsistentVolumeSignalResponse> result = new ArrayList<>(signals);

		return result;
	}

	public void processData(Queue<ConsistentVolumeSignalResponse> signals, List<ConsistentVolumeProjection> bars,
			int baselineWindow, double baselineLowPercentile, double baselineHighPercentile, int rvolPercentileWindow,
			double rvolThresholdPercentile, int consistencyWindow, int requiredScore) {

		int n = bars.size();
		if (n < Math.max(baselineWindow, rvolPercentileWindow)) {
			return;
		}

		// ---------------- Rolling baseline windows ----------------
		Deque<Double> baselineRaw = new ArrayDeque<>();
		SortedWindow baselineSorted = new SortedWindow(baselineWindow);

		// ---------------- RVOL percentile windows ----------------
		Deque<Double> rvolRaw = new ArrayDeque<>();
		SortedWindow rvolSorted = new SortedWindow(rvolPercentileWindow);

		// ---------------- Regime tracking ----------------
		boolean volumeRegimeActive = false;
		int regimeStopPoint = 0;

		int consistencyScore = 0;
		double relativeVolumeSum = 0;

		// Bars inside regime that are NOT spikes (safe to add back later)
		List<Long> normalVolumesDuringRegime = new ArrayList<>();

		// ---------------- Initialize baseline ----------------
		for (int i = 0; i < baselineWindow; i++) {
			double vol = bars.get(i).getVolume();
			baselineRaw.addLast(vol);
			baselineSorted.add(vol);
		}

		// ---------------- Main loop ----------------
		for (int i = baselineWindow; i < n; i++) {

			ConsistentVolumeProjection curr = bars.get(i);

			// ======================================================
			// 1. If regime ended → slide baseline ONLY using normal bars
			// ======================================================
			if (volumeRegimeActive && i >= regimeStopPoint) {

				// Slide baseline forward ONLY with non-spike bars
				for (double vol : normalVolumesDuringRegime) {
					slideBaseline(baselineRaw, baselineSorted, vol, baselineWindow);
				}

				// Reset regime state
				volumeRegimeActive = false;
				consistencyScore = 0;
				relativeVolumeSum = 0;
				normalVolumesDuringRegime.clear();
			}

			// ======================================================
			// 2. Compute baseline mean (filtered)
			// ======================================================
			double baselineMean = baselineSorted.filteredMean(baselineLowPercentile, baselineHighPercentile);

			if (baselineMean <= 0 || Double.isNaN(baselineMean)) {
				slideBaseline(baselineRaw, baselineSorted, curr.getVolume(), baselineWindow);
				continue;
			}

			// ======================================================
			// 3. Compute RVOL
			// ======================================================
			double rvol = curr.getVolume() / baselineMean;

			// ======================================================
			// 4. Determine spike threshold
			// ======================================================
			boolean largeVol = false;

			if (rvolSorted.isFull()) {
				double thresold = rvolSorted.percentile(rvolThresholdPercentile);
				largeVol = rvol >= thresold;
			}

			// ✅ SIMPLE FIX:
			// Only update RVOL distribution if NOT a spike
			if (!largeVol) {

				rvolRaw.addLast(rvol);
				rvolSorted.add(rvol);

				if (rvolRaw.size() > rvolPercentileWindow) {
					double old = rvolRaw.removeFirst();
					rvolSorted.remove(old);
				}
			}

			// ======================================================
			// 5. Regime detection logic
			// ======================================================

			// ---- Start regime on first spike ----
			if (!volumeRegimeActive && largeVol) {

				volumeRegimeActive = true;
				regimeStopPoint = Math.min(i + consistencyWindow, n);

				consistencyScore = 1;
				relativeVolumeSum = rvol;

				normalVolumesDuringRegime.clear();
				continue;
			}

			// ---- Continue regime counting ----
			else if (volumeRegimeActive) {

				if (largeVol) {
					consistencyScore++;
					relativeVolumeSum += rvol;
				} else {
					// Non-spike bar → safe volume → store for later baseline slide
					normalVolumesDuringRegime.add(curr.getVolume());
				}
			}

			// ======================================================
			// 6. Emit signal ONCE when threshold is reached
			// ======================================================
			if (volumeRegimeActive && largeVol && consistencyScore >= requiredScore) {

				ConsistentVolumeSignalResponse signal = new ConsistentVolumeSignalResponse();

				signal.setTicker(curr.getTicker());
				signal.setDate(curr.getDate());

				signal.setConsistencyScore(consistencyScore);
				signal.setConsistencyWindow(consistencyWindow);

				signal.setRelativeVolumesCombinedAverage(relativeVolumeSum / consistencyScore);

				signals.add(signal);
			}

			// ======================================================
			// 7. Slide baseline normally ONLY when regime inactive
			// ======================================================
			if (!volumeRegimeActive) {
				slideBaseline(baselineRaw, baselineSorted, curr.getVolume(), baselineWindow);
			}
		}
	}

	private void slideBaseline(Deque<Double> raw, SortedWindow sorted, double newValue, int maxSize) {

		raw.addLast(newValue);
		sorted.add(newValue);

		if (raw.size() > maxSize) {
			double old = raw.removeFirst();
			sorted.remove(old);
		}
	}
}
