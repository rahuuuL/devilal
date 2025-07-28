package com.terminal_devilal.controllers.Functional.Drawdown.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.terminal_devilal.controllers.DataGathering.DAO.PriceDeliveryVolumeDAO;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.Functional.Drawdown.Model.DrawdownDTO;

@Service
public class DrawdownService {

	private final PriceDeliveryVolumeDAO priceDeliveryVolumeDAO;

	public DrawdownService(PriceDeliveryVolumeDAO priceDeliveryVolumeDAO) {
		super();
		this.priceDeliveryVolumeDAO = priceDeliveryVolumeDAO;
	}

	public List<DrawdownDTO> calculateDrawdowns(String ticker, LocalDate fromDate) {
		List<PriceDeliveryVolume> data = priceDeliveryVolumeDAO
				.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
		return calculateDrawdowns(data);
	}

	public Map<String, List<DrawdownDTO>> calculateDrawdownsForAll(LocalDate fromDate) {
		List<String> tickers = priceDeliveryVolumeDAO.findDistinctTicker();
		Map<String, List<DrawdownDTO>> resultMap = new HashMap<>();

		for (String ticker : tickers) {
			List<PriceDeliveryVolume> data = priceDeliveryVolumeDAO
					.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
			
			double firstPrice = data.get(0).getClose();
			double lastPrice = data.get(data.size() - 1).getClose();

			double drawDownCheck = ((firstPrice - lastPrice) / firstPrice) * 100;
			
			if (!data.isEmpty() && (drawDownCheck < 0) ) {
				resultMap.put(ticker, calculateDrawdowns(data));
			}
			
		}

		return resultMap;
	}

	public List<DrawdownDTO> calculateDrawdowns(List<PriceDeliveryVolume> data) {
		List<DrawdownDTO> results = new ArrayList<>();

		double peak = Double.MIN_VALUE;
		LocalDate peakDate = null;

		LocalDate troughDate = null;
		double trough = Double.MAX_VALUE;

		boolean recovering = false;
		int recoveryDays = 0;

		for (int i = 0; i < data.size(); i++) {
			PriceDeliveryVolume current = data.get(i);
			double close = current.getClose();
			LocalDate date = current.getDate();

			if (close > peak) {
				// Found new peak
				if (troughDate != null && trough < peak) {
					// Add previous drawdown before resetting
					double drawdown = (trough - peak) / peak * 100;
					results.add(new DrawdownDTO(peakDate, peak, troughDate, trough, drawdown, recoveryDays));
				}

				peak = close;
				peakDate = date;
				trough = close;
				troughDate = date;
				recoveryDays = 0;
				recovering = false;

			} else {
				// Below peak
				if (close < trough) {
					trough = close;
					troughDate = date;
					recovering = true;
					recoveryDays = 0;
				} else if (recovering) {
					recoveryDays++;
					if (close >= peak) {
						// Recovered
						double drawdown = (trough - peak) / peak * 100;
						results.add(new DrawdownDTO(peakDate, peak, troughDate, trough, drawdown, recoveryDays));
						// Reset for next cycle
						peak = close;
						peakDate = date;
						trough = close;
						troughDate = date;
						recoveryDays = 0;
						recovering = false;
					}
				}
			}
		}

		return results;
	}

}
