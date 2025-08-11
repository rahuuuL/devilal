package com.terminal_devilal.business_tools.drawdown.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.terminal_devilal.business_tools.drawdown.dto.DrawdownDTO;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.repository.PriceDeliveryVolumeRepository;

@Service
public class DrawdownService {

	private final PriceDeliveryVolumeRepository priceDeliveryVolumeRepository;

	public DrawdownService(PriceDeliveryVolumeRepository priceDeliveryVolumeRepository) {
		super();
		this.priceDeliveryVolumeRepository = priceDeliveryVolumeRepository;
	}

	public List<DrawdownDTO> calculateDrawdowns(String ticker, LocalDate fromDate) {
		List<PriceDeliveryVolumeEntity> data = priceDeliveryVolumeRepository
				.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
		return calculateDrawdowns(data);
	}

	public Map<String, List<DrawdownDTO>> calculateDrawdownsForAll(LocalDate fromDate) {
		List<String> tickers = priceDeliveryVolumeRepository.findDistinctTicker();
		Map<String, List<DrawdownDTO>> resultMap = new HashMap<>();

		for (String ticker : tickers) {
			List<PriceDeliveryVolumeEntity> data = priceDeliveryVolumeRepository
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

	public List<DrawdownDTO> calculateDrawdowns(List<PriceDeliveryVolumeEntity> data) {
		List<DrawdownDTO> results = new ArrayList<>();

		double peak = Double.MIN_VALUE;
		LocalDate peakDate = null;

		LocalDate troughDate = null;
		double trough = Double.MAX_VALUE;

		boolean recovering = false;
		int recoveryDays = 0;

		for (int i = 0; i < data.size(); i++) {
			PriceDeliveryVolumeEntity current = data.get(i);
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
