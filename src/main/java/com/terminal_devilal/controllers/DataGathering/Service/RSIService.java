package com.terminal_devilal.controllers.DataGathering.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.controllers.DataGathering.DAO.RSIDAO;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Model.RSI;

import jakarta.transaction.Transactional;

@Service
public class RSIService {

	private final RSIDAO rsidao;
	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	public RSIService(RSIDAO rsidao, PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.rsidao = rsidao;
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}

	@Transactional
	public void saveAllRSI(List<RSI> rsis) {
		this.rsidao.saveAll(rsis);
	}

	@Transactional
	public void saveRSI(RSI rsis) {
		this.rsidao.save(rsis);
	}

	public List<RSI> getAllRSIByDate(LocalDate date) {
		return rsidao.findByDate(date);
	}

	public List<RSI> getRSIFromDate(String ticker, LocalDate fromDate) {
		return rsidao.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
	}

	public void processRSI(JsonNode jsonNode) {
		PriceDeliveryVolume pdv = this.priceDeliveryVolumeService.parseStockData(jsonNode);

		// Calc close diff
		double closeDiff = pdv.getClose() - pdv.getPrevoiusClosePrice();

		// Get RSI for 14
		List<RSI> FourtheenDayData = this.rsidao.findRecent14RSIs(pdv.getTicker());
		double FourtheenDataRSI = FourtheenDayData.size() == 14 ? calculateRSI(FourtheenDayData) : 0;

		// Get RSI for 21 days
		List<RSI> TwentyOneDayData = this.rsidao.findRecent21RSIs(pdv.getTicker());

		double TwentyOneDayRSI = TwentyOneDayData.size() == 21 ? calculateRSI(TwentyOneDayData) : 0;

		// Save RSI
		RSI rsi = new RSI(pdv.getTicker(), pdv.getDate(), closeDiff, FourtheenDataRSI, TwentyOneDayRSI);
		saveRSI(rsi);
	}

	private double calculateRSI(List<RSI> rsiData) {
		double gainSum = 0.0;
		double lossSum = 0.0;
		int gainCount = 0;
		int lossCount = 0;

		for (RSI data : rsiData) {
			double change = data.getCloseDiff();
			if (change > 0) {
				gainSum += change;
				gainCount++;
			} else if (change < 0) {
				lossSum += Math.abs(change);
				lossCount++;
			}
		}

		double averageGain = gainCount > 0 ? gainSum / gainCount : 0;
		double averageLoss = lossCount > 0 ? lossSum / lossCount : 0;

		if (averageLoss == 0) {
			return 100.0; // Max RSI
		}

		double rs = averageGain / averageLoss;
		return 100.0 - (100.0 / (1 + rs));
	}
}
