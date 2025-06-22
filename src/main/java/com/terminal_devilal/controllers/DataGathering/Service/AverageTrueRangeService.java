package com.terminal_devilal.controllers.DataGathering.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.controllers.DataGathering.DAO.AverageTrueRangeDAO;
import com.terminal_devilal.controllers.DataGathering.Model.AverageTrueRange;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;

import jakarta.transaction.Transactional;

@Service
public class AverageTrueRangeService {

	private AverageTrueRangeDAO averageTrueRangeDAO;
	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	public AverageTrueRangeService(AverageTrueRangeDAO averageTrueRangeDAO,
			PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.averageTrueRangeDAO = averageTrueRangeDAO;
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}

	@Transactional
	public void saveAllATR(List<AverageTrueRange> averageTrueRanges) {
		this.averageTrueRangeDAO.saveAll(averageTrueRanges);
	}

	@Transactional
	public void saveATR(AverageTrueRange averageTrueRanges) {
		this.averageTrueRangeDAO.save(averageTrueRanges);
	}

	public List<AverageTrueRange> getATRForTickerFromDate(String ticker, LocalDate fromDate) {
		return averageTrueRangeDAO.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
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
		PriceDeliveryVolume pdv = this.priceDeliveryVolumeService.parseStockData(jsonNode);

		// Calc ATR
		double trueRange = calculateTrueRange(pdv.getHigh(), pdv.getLow(), pdv.getPrevoiusClosePrice());

		// Make ATR object and add to list
		AverageTrueRange averageTrueRange = new AverageTrueRange(pdv.getTicker(), pdv.getDate(), trueRange);
		saveATR(averageTrueRange);

	}

}
