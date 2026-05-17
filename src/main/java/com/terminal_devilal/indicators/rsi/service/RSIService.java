package com.terminal_devilal.indicators.rsi.service;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.rsi.dto.RsiPercentileDTO;
import com.terminal_devilal.indicators.rsi.entities.RSIEntity;
import com.terminal_devilal.indicators.rsi.entities.projections.RsiPercentileProjection;
import com.terminal_devilal.indicators.rsi.entities.projections.RsiProjection;
import com.terminal_devilal.indicators.rsi.repository.RSIRepository;
import com.terminal_devilal.utils.resilientbatchservice.ResilientBatchService;

import jakarta.transaction.Transactional;

@Service
public class RSIService extends ResilientBatchService<RSIEntity> {

	private final RSIRepository rSIRepository;

	// ─── In-memory sliding window cache ───────────────────────────────────────
	// Key: ticker symbol
	// Value: ordered deque of up to 21 RSIEntity entries, oldest at head, newest at
	// tail
	private final ConcurrentHashMap<String, ArrayDeque<RSIEntity>> rsiCache = new ConcurrentHashMap<>();
	private static final int CACHE_MAX = 21;

	public RSIService(RSIRepository rSIRepository) {
		super();
		this.rSIRepository = rSIRepository;
	}

	@Transactional
	public void saveAllRSI(List<RSIEntity> rsis) {
		this.rSIRepository.saveAll(rsis);
	}

	@Transactional
	public void saveRSI(RSIEntity rsis) {
		this.rSIRepository.save(rsis);
	}

	public List<RSIEntity> getAllRSIByDate(LocalDate date) {
		return rSIRepository.findByDate(date);
	}

	public List<RsiProjection> getRSIWithinDatesForTickers(List<String> tickers, LocalDate fromDate, LocalDate toDate) {
		return rSIRepository.findByTickerInAndDateBetween(tickers, fromDate, toDate);
	}

	public void processRSI(PriceDeliveryVolumeEntity pdv) {
		String ticker = pdv.getTicker();
		double closeDiff = pdv.getClose() - pdv.getPrevoiusClosePrice();

		ArrayDeque<RSIEntity> window = rsiCache.computeIfAbsent(ticker, t -> {
			List<RSIEntity> dbData = rSIRepository.findRecent21RSIs(ticker, pdv.getDate());
			return new ArrayDeque<>(dbData);
		});

		RSIEntity newEntity;

		// Single synchronized block — calculate AND slide atomically
		// Only locks this ticker's window, other tickers process freely in parallel
		synchronized (window) {
			List<RSIEntity> snapshot = new ArrayList<>(window);
			int size = snapshot.size();

			double rsi14 = size >= 14 ? calculateRSI(snapshot.subList(size - 14, size)) : 0;
			double rsi21 = size == CACHE_MAX ? calculateRSI(snapshot) : 0;

			newEntity = new RSIEntity(ticker, pdv.getDate(), closeDiff, rsi14, rsi21);

			if (window.size() >= CACHE_MAX)
				window.pollFirst();
			window.addLast(newEntity);
		}
		// enqueue is outside the lock — no need to hold it while adding to the buffer
		enqueue(newEntity);
	}

	// ─── RSI calculation ─────────────────────────────────────────────────────

	private double calculateRSI(List<RSIEntity> rsiData) {
		double gainSum = 0.0;
		double lossSum = 0.0;

		for (RSIEntity data : rsiData) {
			double change = data.getCloseDiff();
			if (change > 0) {
				gainSum += change;
			} else if (change < 0) {
				lossSum += Math.abs(change);
			}
		}

		double averageGain = gainSum / rsiData.size();
		double averageLoss = lossSum / rsiData.size();

		if (averageLoss == 0)
			return 100.0;

		double rs = averageGain / averageLoss;
		return 100.0 - (100.0 / (1 + rs));
	}

	// ─── ResilientBatchService implementation ─────────────────────────────────

	@Override
	protected void saveAll(List<RSIEntity> batch) {
		rSIRepository.saveAll(batch);
	}

	@Override
	protected void saveOne(RSIEntity record) {
		rSIRepository.save(record);
	}

	// Optional: override to plug in metrics/alerting for permanent failures
	@Override
	protected void onPermanentFailure(RSIEntity record, Exception e) {
		System.err.printf("[RSI][PERMANENT_FAILURE] ticker=%s date=%s closeDiff=%s error=%s%n", record.getTicker(),
				record.getDate(), record.getCloseDiff(), e.getMessage());
	}

	public List<RsiPercentileDTO> computeRsiPercentiles(LocalDate fromDate, LocalDate toDate, boolean use14DayRsi) {

		List<RsiPercentileProjection> rows = rSIRepository.findPercentileRecordsBetweenDates(fromDate, toDate);

		// 1️⃣ Group by ticker
		Map<String, List<RsiPercentileProjection>> byTicker = rows.stream()
				.collect(Collectors.groupingBy(RsiPercentileProjection::getTicker));

		List<RsiPercentileDTO> result = new ArrayList<>();

		for (Map.Entry<String, List<RsiPercentileProjection>> entry : byTicker.entrySet()) {

			String ticker = entry.getKey();
			List<RsiPercentileProjection> data = entry.getValue();

			// 2️⃣ Find RSI value on toDate
			Optional<RsiPercentileProjection> endDateRow = data.stream().filter(d -> d.getDate().equals(toDate))
					.findFirst();

			if (endDateRow.isEmpty())
				continue;

			double targetRsi = use14DayRsi ? endDateRow.get().getFourtheenDaysRSI()
					: endDateRow.get().getTweentyOneDaysRSI();

			// 3️⃣ Collect RSI values
			List<Double> rsiSeries = data.stream()
					.map(d -> use14DayRsi ? d.getFourtheenDaysRSI() : d.getTweentyOneDaysRSI()).sorted().toList();

			// 4️⃣ Percentile calculation
			long countBelow = rsiSeries.stream().filter(v -> v < targetRsi).count();

			double percentile = (double) countBelow / rsiSeries.size() * 100.0;

			// 5️⃣ Build DTO (only RSI-related fields)
			RsiPercentileDTO dto = new RsiPercentileDTO();
			dto.setTicker(ticker);
			dto.setRsiValue(targetRsi);
			dto.setPercentile(percentile);

			// 6️⃣ Enrich with sector + trade info

			result.add(dto);
		}

		// 7 Sort DESC by percentile
		return result;
	}
}
