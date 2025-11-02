package com.terminal_devilal.indicators.pdv.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.entities.StockClosePrice;
import com.terminal_devilal.indicators.pdv.repository.PriceDeliveryVolumeRepository;

@Service
public class PriceDeliveryVolumeService {

	private final PriceDeliveryVolumeRepository repository;

	private final PriceDeliveryVolumeUtility utils;

	public PriceDeliveryVolumeService(PriceDeliveryVolumeRepository repository, PriceDeliveryVolumeUtility utils) {
		super();
		this.repository = repository;
		this.utils = utils;
	}

	@Transactional
	public void savePdv(PriceDeliveryVolumeEntity data) {
		repository.save(data);
	}

	@Transactional
	public void saveAllPdvList(List<PriceDeliveryVolumeEntity> dataList) {
		repository.saveAll(dataList);
	}

	public List<PriceDeliveryVolumeEntity> getAllPdvWithinDate(String Ticker, LocalDate FromDate, LocalDate ToDate) {
		return repository.findByTickerAndDateBetween(Ticker, ToDate, ToDate);
	}

	public List<PriceDeliveryVolumeEntity> getLatestRecordForTickers(List<String> tickers) {
		return repository.findLatestRecordForTickers(tickers);
	}

	/**
	 * Get all stocks/tickers close prices grouped by stock/ticker
	 */
	public Map<String, List<Double>> getGroupedClosePrices(LocalDate fromDate) {
		return repository.getClosePrices(fromDate).stream()
				// Group prices by ticker
				.collect(Collectors.groupingBy(StockClosePrice::getTicker, // Key: ticker
						Collectors.mapping(StockClosePrice::getClose, Collectors.toList()) // Value: List of close
																							// prices
				));
	}

	/**
	 * Get close prices for a specific stocks from a specific date.
	 */
	public Map<String, List<Double>> getClosePricesForTickerSince(LocalDate fromDate, List<String> tickers) {
		return repository.getClosePricesForStocks(fromDate, tickers).stream().collect(Collectors.groupingBy(
				StockClosePrice::getTicker, Collectors.mapping(StockClosePrice::getClose, Collectors.toList())));
	}
	
	/**
	 * Get close prices for a specific stocks from a specific date.
	 */
	public Map<String, List<PriceDeliveryVolumeEntity>> getPDVForTickerSince(LocalDate fromDate,
			List<String> tickers) {
		return repository.getPDVForTickers(fromDate, tickers).stream()
				.collect(Collectors.groupingBy(PriceDeliveryVolumeEntity::getTicker));
	}

	// Mapper Utility function
	public TreeSet<PriceDeliveryVolumeEntity> parseStockData(JsonNode node, String ticker) {

		TreeSet<PriceDeliveryVolumeEntity> stockList = new TreeSet<PriceDeliveryVolumeEntity>(
				Comparator.comparing(PriceDeliveryVolumeEntity::getDate));
		JsonNode dataArray = node.path("data");

		if (!dataArray.isArray()) {
			System.out.print("Empty date" + node.toPrettyString());
			return stockList; // return empty if "data" is not an array
		}

		for (JsonNode item : dataArray) {
			PriceDeliveryVolumeEntity stock = utils.parseStockData(item);
			stock.setTicker(ticker);
			stockList.add(stock);
		}
		return stockList;
	}

}