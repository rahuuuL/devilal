package com.terminal_devilal.indicators.volume.service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.common.service.TickerInfoService;
import com.terminal_devilal.indicators.pdv.entities.projections.ConsistentVolumeProjection;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.indicators.volume.model.ConsistentVolumeSignalResponse;
import com.terminal_devilal.indicators.volume.utils.SortedWindow;

@Service
public class ConsistentVolumeDetector {

	private final PriceDeliveryVolumeService priceVolume;

	private final TickerInfoService companyDetails;

	public ConsistentVolumeDetector(PriceDeliveryVolumeService priceVolume, TickerInfoService companyDetails) {
		super();
		this.priceVolume = priceVolume;
		this.companyDetails = companyDetails;
	}

	public List<ConsistentVolumeSignalResponse> detectConsistentVolumes(LocalDate fromDate, LocalDate toDate,
			int inputBaselineWindow, double baselineLowPercentile, double baselineHighPercentile,
			int baseRvolPercentileWindow, double rvolThresholdPercentile, int consistencyWindow, int requiredScore) {

		// -------- Validation --------
		int baselineWindow = Math.max(30, inputBaselineWindow);
		int rvolPercentileWindow = Math.max(100, baseRvolPercentileWindow);

		// -------- Fetch data --------
		List<ConsistentVolumeProjection> allData = priceVolume.getAllBetweenTwoDates(fromDate, toDate);

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

		Map<String, Long> tickerCountMap = result.stream()
				.collect(Collectors.groupingBy(ConsistentVolumeSignalResponse::getTicker, Collectors.counting()));

		result.sort(Comparator
				// 1️⃣ Market cap DESC
				.comparing(ConsistentVolumeSignalResponse::getTotalMarketCap, Comparator.reverseOrder())

				// 2️⃣ Occurrence count DESC
				.thenComparing(r -> tickerCountMap.getOrDefault(r.getTicker(), 0L), Comparator.reverseOrder())

				// 3️⃣ Date DESC
				.thenComparing(ConsistentVolumeSignalResponse::getDate, Comparator.reverseOrder()));

		return result;
	}

	public void processData(Queue<ConsistentVolumeSignalResponse> signals, List<ConsistentVolumeProjection> bars,
			int baselineWindow, double baselineLowPercentile, double baselineHighPercentile, int rvolPercentileWindow,
			double rvolThresholdPercentile, int consistencyWindow, int requiredScore) {

		int n = bars.size();
		if (n < Math.max(baselineWindow, rvolPercentileWindow)) {
			return;
		}

		// -------- Time-order windows --------
		Deque<Double> baselineRaw = new ArrayDeque<>();
		Deque<Double> rvolRaw = new ArrayDeque<>();
		Deque<Boolean> directionalWindow = new ArrayDeque<>();

		// -------- Distribution windows --------
		SortedWindow baselineSorted = new SortedWindow(baselineWindow);
		SortedWindow rvolSorted = new SortedWindow(rvolPercentileWindow);

		int consistencyScore = 0;

		// -------- Initialize baseline --------
		for (int i = 0; i < baselineWindow; i++) {
			double vol = bars.get(i).getVolume();
			baselineRaw.addLast(vol);
			baselineSorted.add(vol);
		}

		// -------- Main loop --------
		for (int i = baselineWindow; i < n; i++) {

			ConsistentVolumeProjection curr = bars.get(i);

			// ---- Baseline filtered mean ----
			double baselineMean = baselineSorted.filteredMean(baselineLowPercentile, baselineHighPercentile);

			if (baselineMean <= 0 || Double.isNaN(baselineMean)) {
				slideBaseline(baselineRaw, baselineSorted, curr.getVolume(), baselineWindow);
				continue;
			}

			// ---- RVOL ----
			double rvol = curr.getVolume() / baselineMean;

			rvolRaw.addLast(rvol);
			rvolSorted.add(rvol);

			if (rvolRaw.size() > rvolPercentileWindow) {
				double old = rvolRaw.removeFirst();
				rvolSorted.remove(old);
			}

			boolean largeVol = false;
			if (rvolSorted.isFull()) {
				double pX = rvolSorted.percentile(rvolThresholdPercentile);
				largeVol = rvol >= pX;
			}

			// ---- Pure volume confirmation (NO direction) ----
			boolean volumeEvent = largeVol;

			directionalWindow.addLast(volumeEvent);
			if (volumeEvent) {
				consistencyScore++;
			}

			boolean volumeRegimeActive = consistencyScore > 0;

			if (directionalWindow.size() > consistencyWindow) {
				if (directionalWindow.removeFirst()) {
					consistencyScore--;
				}
			}

			// ---- Emit signal ----
			if (consistencyScore >= requiredScore) {
				ConsistentVolumeSignalResponse r = new ConsistentVolumeSignalResponse();
				r.setTicker(curr.getTicker());
				r.setDate(curr.getDate());
				r.setVolume(curr.getVolume());
				r.setRvol(rvol);
				r.setConsistencyScore(consistencyScore);
				r.setConsistencyWindow(consistencyWindow);
				r.setRequiredScore(requiredScore);
				r.setSignal(true);

				signals.add(companyDetails.enrichTickerDetails(curr.getTicker(), r));
			}

			// ---- Slide baseline (NO lookahead bias) ----
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
