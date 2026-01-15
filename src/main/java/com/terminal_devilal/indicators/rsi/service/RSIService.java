package com.terminal_devilal.indicators.rsi.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.common.service.TickerInfoService;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeUtility;
import com.terminal_devilal.indicators.rsi.dto.RsiPercentileDTO;
import com.terminal_devilal.indicators.rsi.entities.RSIEntity;
import com.terminal_devilal.indicators.rsi.repository.RSIRepository;

import jakarta.transaction.Transactional;

@Service
public class RSIService {

	private final RSIRepository rSIRepository;
	private final PriceDeliveryVolumeUtility priceDeliveryVolumeUtility;
	private final TickerInfoService companyDetails;

	public RSIService(RSIRepository rSIRepository, PriceDeliveryVolumeUtility priceDeliveryVolumeUtility,
			TickerInfoService companyDetails) {
		super();
		this.rSIRepository = rSIRepository;
		this.priceDeliveryVolumeUtility = priceDeliveryVolumeUtility;
		this.companyDetails = companyDetails;
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

	public List<RSIEntity> getRSIFromDate(String ticker, LocalDate fromDate) {
		return rSIRepository.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
	}

	public void processRSI(JsonNode jsonNode) {
		PriceDeliveryVolumeEntity pdv = this.priceDeliveryVolumeUtility.parseStockData(jsonNode);

		// Calc close diff
		double closeDiff = pdv.getClose() - pdv.getPrevoiusClosePrice();

		// Get RSI for 14
		List<RSIEntity> FourtheenDayData = this.rSIRepository.findRecent14RSIs(pdv.getTicker(), pdv.getDate());
		double FourtheenDataRSI = FourtheenDayData.size() == 14 ? calculateRSI(FourtheenDayData) : 0;

		// Get RSI for 21 days
		List<RSIEntity> TwentyOneDayData = this.rSIRepository.findRecent21RSIs(pdv.getTicker(), pdv.getDate());
		double TwentyOneDayRSI = TwentyOneDayData.size() == 21 ? calculateRSI(TwentyOneDayData) : 0;

		// Save RSI
		RSIEntity rSIEntity = new RSIEntity(pdv.getTicker(), pdv.getDate(), closeDiff, FourtheenDataRSI,
				TwentyOneDayRSI);
		saveRSI(rSIEntity);
	}

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

		// Avoid division by zero
		if (averageLoss == 0) {
			return 100.0; // RSI max
		}

		double rs = averageGain / averageLoss;
		return 100.0 - (100.0 / (1 + rs));
	}

	public List<RsiPercentileDTO> computeRsiPercentiles(LocalDate fromDate, LocalDate toDate, boolean use14DayRsi) {

		List<RSIEntity> rows = rSIRepository.findAllBetweenDates(fromDate, toDate);

		// 1️⃣ Group by ticker
		Map<String, List<RSIEntity>> byTicker = rows.stream().collect(Collectors.groupingBy(RSIEntity::getTicker));

		List<RsiPercentileDTO> result = new ArrayList<>();

		for (Map.Entry<String, List<RSIEntity>> entry : byTicker.entrySet()) {

			String ticker = entry.getKey();
			List<RSIEntity> data = entry.getValue();

			// 2️⃣ Find RSI value on toDate
			Optional<RSIEntity> endDateRow = data.stream().filter(d -> d.getDate().equals(toDate)).findFirst();

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
			companyDetails.enrichTickerDetails(ticker, dto);

			result.add(dto);
		}

		// 7 Sort DESC by percentile
		return result.stream()
				.sorted(Comparator
						.comparing(RsiPercentileDTO::getTotalMarketCap, Comparator.nullsLast(Double::compareTo))
						.reversed().thenComparing(RsiPercentileDTO::getPercentile, Comparator.reverseOrder()))
				.toList();
	}
}
