package com.terminal_devilal.indicators.pdv.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;

@RestController
@RequestMapping("/api/devilal/pdv")
public class PDVController {

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	public PDVController(PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}

	@GetMapping("/getprices")
	public List<PriceDeliveryVolumeEntity> getHeatMap(@RequestParam("tickers") List<String> tickers) {
		return priceDeliveryVolumeService.getLatestRecordForTickers(tickers);
	}

	@GetMapping("/price-volume-data")
	public Map<String, List<Double>> getClosePricesSinceDate(@RequestParam("tickers") List<String> tickers,
			@RequestParam("fromDate") LocalDate fromDate) {
		return priceDeliveryVolumeService.getClosePricesForTickerSince(fromDate, tickers);
	}

}
