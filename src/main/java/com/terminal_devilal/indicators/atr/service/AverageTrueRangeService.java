package com.terminal_devilal.indicators.atr.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.indicators.atr.entities.AverageTrueRangeEntity;
import com.terminal_devilal.indicators.atr.repository.AverageTrueRangeRepository;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeUtility;

import jakarta.transaction.Transactional;

@Service
public class AverageTrueRangeService {

	private AverageTrueRangeRepository averageTrueRangeRepository;
	private final PriceDeliveryVolumeUtility priceDeliveryVolumeUtility;

	public AverageTrueRangeService(AverageTrueRangeRepository averageTrueRangeRepository,
			PriceDeliveryVolumeUtility priceDeliveryVolumeUtility) {
		super();
		this.averageTrueRangeRepository = averageTrueRangeRepository;
		this.priceDeliveryVolumeUtility = priceDeliveryVolumeUtility;
	}

	@Transactional
	public void saveAllATR(List<AverageTrueRangeEntity> averageTrueRangeEntities) {
		this.averageTrueRangeRepository.saveAll(averageTrueRangeEntities);
	}

	@Transactional
	public void saveATR(AverageTrueRangeEntity averageTrueRangeEntities) {
		this.averageTrueRangeRepository.save(averageTrueRangeEntities);
	}

	public List<AverageTrueRangeEntity> getATRForTickerFromDate(String ticker, LocalDate fromDate) {
		return averageTrueRangeRepository.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
	}

	/**
	 * Calculates the True Range (TR) for a single period.
	 *
	 * @param high      Today's high price
	 * @param low       Today's low price
	 * @param prevClose Previous day's closing price
	 * @return True Range
	 */
	public double calculateTrueRange(double high, double low, double prevClose) {
		double range1 = high - low;
		double range2 = Math.abs(high - prevClose);
		double range3 = Math.abs(low - prevClose);

		return Math.max(range1, Math.max(range2, range3));
	}

	public void processATR(JsonNode jsonNode) {
		PriceDeliveryVolumeEntity pdv = this.priceDeliveryVolumeUtility.parseStockData(jsonNode);

		// Calc ATR
		double trueRange = calculateTrueRange(pdv.getHigh(), pdv.getLow(), pdv.getPrevoiusClosePrice());

		// Make ATR object and add to list
		AverageTrueRangeEntity averageTrueRangeEntity = new AverageTrueRangeEntity(pdv.getTicker(), pdv.getDate(), trueRange);
		saveATR(averageTrueRangeEntity);

	}

}
