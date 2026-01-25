package com.terminal_devilal.indicators.volume.service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.common.service.TickerInfoService;
import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.indicators.volume.model.ConsistentVolumeSignalResponse;

@Service
public class ConsistentVolumeDetector {

	private final PriceDeliveryVolumeService priceVolume;

	private final DataFetchHistoryService dfht;

	private final TickerInfoService companyDetails;

	public ConsistentVolumeDetector(PriceDeliveryVolumeService priceVolume, DataFetchHistoryService dfht,
			TickerInfoService companyDetails) {
		super();
		this.priceVolume = priceVolume;
		this.dfht = dfht;
		this.companyDetails = companyDetails;
	}

	public List<ConsistentVolumeSignalResponse> detectConsistantVolumes(LocalDate fromDate, LocalDate toDate,
			int window, int inputBaselineWindow, double baselineLowPercentile, double baselineHighPercentile,
			int baseRvolPercentileWindow, double rvolThresholdPercentile, int consistencyWindow, int requiredScore) {

		int baselineWindow = Math.max(30, inputBaselineWindow);
		int rvolPercentileWindow = Math.max(100, baseRvolPercentileWindow);

		Queue<ConsistentVolumeSignalResponse> signals = new ConcurrentLinkedQueue<>();

		List<String> tickers = dfht.getAllTickers();

		// ✅ CHUNKED DB FETCH (THIS FIXES THE ERROR)
		List<PriceDeliveryVolumeEntity> allData = fetchInChunks(fromDate, toDate, tickers, window);

		Map<String, List<PriceDeliveryVolumeEntity>> dataByTicker = allData.stream().collect(
				Collectors.groupingBy(PriceDeliveryVolumeEntity::getTicker, LinkedHashMap::new, Collectors.toList()));

		// ✅ CPU PARALLELISM (SAFE)
		dataByTicker.entrySet().parallelStream().forEach(entry -> {

			String ticker = entry.getKey();
			List<PriceDeliveryVolumeEntity> bars = entry.getValue();
			int n = bars.size();

			if (n < Math.max(baselineWindow, rvolPercentileWindow))
				return;

			Deque<Double> baselineVolumeWindow = new ArrayDeque<>();
			Deque<Double> rvolWindow = new ArrayDeque<>();
			Deque<Boolean> directionalWindow = new ArrayDeque<>();

			int consistencyScore = 0;

			for (int i = 0; i < baselineWindow; i++) {
				baselineVolumeWindow.addLast((double) bars.get(i).getVolume());
			}

			for (int i = baselineWindow; i < n; i++) {

				PriceDeliveryVolumeEntity curr = bars.get(i);

				double baselineMean = percentileFilteredMean(new ArrayList<>(baselineVolumeWindow),
						baselineLowPercentile, baselineHighPercentile);

				if (baselineMean <= 0 || Double.isNaN(baselineMean)) {
					slideBaseline(baselineVolumeWindow, curr.getVolume());
					continue;
				}

				double rvol = curr.getVolume() / baselineMean;

				rvolWindow.addLast(rvol);
				if (rvolWindow.size() > rvolPercentileWindow)
					rvolWindow.removeFirst();

				boolean largeVol = false;
				if (rvolWindow.size() == rvolPercentileWindow) {
					double p90 = percentile(new ArrayList<>(rvolWindow), rvolThresholdPercentile);
					largeVol = rvol >= p90;
				}

				boolean directionalLargeVol = largeVol && (curr.getClose() > curr.getVwap()
						|| (curr.getClose() > curr.getOpen() && curr.getClose() > curr.getPrevoiusClosePrice()));

				directionalWindow.addLast(directionalLargeVol);
				if (directionalLargeVol)
					consistencyScore++;

				if (directionalWindow.size() > consistencyWindow) {
					if (directionalWindow.removeFirst())
						consistencyScore--;
				}

				if (consistencyScore >= requiredScore) {
					ConsistentVolumeSignalResponse r = new ConsistentVolumeSignalResponse();
					r.setTicker(ticker);
					r.setDate(curr.getDate());
					r.setVolume(curr.getVolume());
					r.setRvol(rvol);
					r.setConsistencyScore(consistencyScore);
					r.setConsistencyWindow(consistencyWindow);
					r.setRequiredScore(requiredScore);
					r.setSignal(true);

					signals.add(companyDetails.enrichTickerDetails(ticker, r));
				}

				slideBaseline(baselineVolumeWindow, curr.getVolume());
			}
		});

		List<ConsistentVolumeSignalResponse> result = new ArrayList<>(signals);

		result.sort(Comparator.comparing(ConsistentVolumeSignalResponse::getTotalMarketCap, Comparator.reverseOrder())
				.thenComparing(ConsistentVolumeSignalResponse::getConsistencyScore, Comparator.reverseOrder())
				.thenComparing(ConsistentVolumeSignalResponse::getRvol, Comparator.reverseOrder()));

		return result;
	}

	private List<PriceDeliveryVolumeEntity> fetchInChunks(LocalDate fromDate, LocalDate toDate, List<String> tickers,
			int window) {

		final int CHUNK_SIZE = 50; // 🔑 tune based on executor

		List<PriceDeliveryVolumeEntity> allData = new ArrayList<>();

		for (List<String> tickerChunk : chunk(tickers, CHUNK_SIZE)) {

			// ⚠️ sequential call — prevents executor overload
			List<PriceDeliveryVolumeEntity> chunkData = priceVolume.getClosePricesWithDateRange(fromDate, toDate,
					tickerChunk, window);

			allData.addAll(chunkData);
		}

		return allData;
	}

	private static <T> List<List<T>> chunk(List<T> list, int chunkSize) {
		List<List<T>> chunks = new ArrayList<>();
		for (int i = 0; i < list.size(); i += chunkSize) {
			chunks.add(list.subList(i, Math.min(i + chunkSize, list.size())));
		}
		return chunks;
	}

	private void slideBaseline(Deque<Double> window, double newVolume) {
		window.removeFirst();
		window.addLast(newVolume);
	}

	private double percentile(List<Double> values, double percentile) {
		if (values.isEmpty())
			return Double.NaN;

		List<Double> sorted = new ArrayList<>(values);
		Collections.sort(sorted);

		int index = (int) Math.ceil(percentile * sorted.size()) - 1;
		index = Math.max(0, Math.min(index, sorted.size() - 1));

		return sorted.get(index);
	}

	private double percentileFilteredMean(List<Double> values, double lowP, double highP) {
		if (values.isEmpty())
			return Double.NaN;

		double pLow = percentile(values, lowP);
		double pHigh = percentile(values, highP);

		double sum = 0.0;
		int count = 0;

		for (double v : values) {
			if (v >= pLow && v <= pHigh) {
				sum += v;
				count++;
			}
		}
		return count == 0 ? Double.NaN : sum / count;
	}

}
