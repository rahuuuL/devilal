package com.terminal_devilal.indicators.pdv.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.entity.projections.PriceOhlcvProjection;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;

@RestController
@RequestMapping("/api/devilal/pdv")
public class PDVController {

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	public PDVController(PriceDeliveryVolumeService priceDeliveryVolumeService) {
		super();
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}

	@GetMapping("/within-dates")
	public List<PriceOhlcvProjection> getPDV(
			@RequestParam(value = "fromDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(value = "toDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam("tickers") List<String> tickers) {
		return priceDeliveryVolumeService.getAllPdvWithinDate(tickers, fromDate, toDate);
	}

	@GetMapping("/price-volume-data")
	public Map<String, List<PriceDeliveryVolumeEntity>> getClosePricesSinceDate(
			@RequestParam("tickers") List<String> tickers, @RequestParam("fromDate") LocalDate fromDate) {
		return priceDeliveryVolumeService.getPDVForTickerSince(fromDate, tickers);
	}

	@GetMapping("/latest-price-volume-data")
	public List<PriceOhlcvProjection> getLatestRecordForTickers(@RequestParam("tickers") List<String> tickers) {
		return priceDeliveryVolumeService.getLatestRecordForTickers(tickers);
	}

}
