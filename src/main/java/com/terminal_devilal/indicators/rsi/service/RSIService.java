package com.terminal_devilal.indicators.rsi.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeUtility;
import com.terminal_devilal.indicators.rsi.dto.ConsecutiveRSIAnalysis;
import com.terminal_devilal.indicators.rsi.entities.RSIEntity;
import com.terminal_devilal.indicators.rsi.repository.RSIRepository;

import jakarta.transaction.Transactional;

@Service
public class RSIService {

	private final RSIRepository rSIRepository;
	private final PriceDeliveryVolumeUtility priceDeliveryVolumeUtility;

	public RSIService(RSIRepository rSIRepository, PriceDeliveryVolumeUtility priceDeliveryVolumeUtility) {
		super();
		this.rSIRepository = rSIRepository;
		this.priceDeliveryVolumeUtility = priceDeliveryVolumeUtility;
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

	public List<ConsecutiveRSIAnalysis> trackRSITrend(List<String> tickers, int days, LocalDate cutOff,
			double rsiThreshold) {
		List<RSIEntity> data = new ArrayList<RSIEntity>();
		if (tickers.size() == 0) {
			data = rSIRepository.trackAllRSIData(cutOff, days);
		} else {
			data = rSIRepository.trackRSIData(tickers, cutOff, days);

		}

		return data.stream().collect(Collectors.groupingBy(RSIEntity::getTicker)).entrySet().stream().map(entry -> {
			boolean allAboveThreshold = entry.getValue().stream().mapToDouble(RSIEntity::getFourtheenDaysRSI)
					.allMatch(rsi -> rsi > rsiThreshold);

			if (!allAboveThreshold) {
				return null;
			}

			List<Double> rsiList = entry.getValue().stream().map(RSIEntity::getFourtheenDaysRSI).toList();

			return new ConsecutiveRSIAnalysis(entry.getKey(), rsiList);
		}).filter(Objects::nonNull).toList();
	}
}
