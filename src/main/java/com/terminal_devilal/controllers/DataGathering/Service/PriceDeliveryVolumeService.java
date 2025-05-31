package com.terminal_devilal.controllers.DataGathering.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.Utils.KafkaUtils;
import com.terminal_devilal.controllers.DataGathering.DAO.PriceDeliveryVolumeDAO;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;

@Service
public class PriceDeliveryVolumeService {

	private final PriceDeliveryVolumeDAO repository;

	private final KafkaUtils kafkaUtils;

	public PriceDeliveryVolumeService(PriceDeliveryVolumeDAO repository, KafkaUtils kafkaUtils) {
		super();
		this.repository = repository;
		this.kafkaUtils = kafkaUtils;
	}

	@Transactional
	public void savePdv(PriceDeliveryVolume data) {
		repository.save(data);
	}

	@Transactional
	public void saveAllPdvList(List<PriceDeliveryVolume> dataList) {
		repository.saveAll(dataList);
	}

	public List<PriceDeliveryVolume> getAllPdvWithinDate(String Ticker, LocalDate FromDate, LocalDate ToDate) {
		return repository.findByTickerAndDateBetween(Ticker, ToDate, ToDate);
	}

	// Static date formatter reused to avoid re-creating it every time
	private final DateTimeFormatter NSE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

	// Mapper Utility function
	public TreeSet<PriceDeliveryVolume> parseStockData(JsonNode node, String ticker) {

		TreeSet<PriceDeliveryVolume> stockList = new TreeSet<PriceDeliveryVolume>(
				Comparator.comparing(PriceDeliveryVolume::getDate));
		JsonNode dataArray = node.path("data");

		if (!dataArray.isArray()) {
			System.out.print("Empty date" + node.toPrettyString());
			return stockList; // return empty if "data" is not an array
		}

		AtomicReference<String> lastTicker = new AtomicReference<>(null);

		for (JsonNode item : dataArray) {
			PriceDeliveryVolume stock = new PriceDeliveryVolume();

			stock.setTicker(ticker);
			stock.setDate(parseDate(item.path("mTIMESTAMP").asText("")));
			stock.setHigh(item.path("CH_TRADE_HIGH_PRICE").asDouble(0.0));
			stock.setLow(item.path("CH_TRADE_LOW_PRICE").asDouble(0.0));
			stock.setOpen(item.path("CH_OPENING_PRICE").asDouble(0.0));
			stock.setClose(item.path("CH_CLOSING_PRICE").asDouble(0.0));
			stock.setLastTradeValue(item.path("CH_LAST_TRADED_PRICE").asDouble(0.0));
			stock.setPrevoiusClosePrice(item.path("CH_PREVIOUS_CLS_PRICE").asDouble(0.0));
			stock.setVolume(item.path("CH_TOT_TRADED_QTY").asLong(0L));
			stock.setValue(item.path("CH_TOT_TRADED_VAL").asDouble(0.0));
			stock.setTrades(item.path("CH_TOTAL_TRADES").asInt(0));
			stock.setDeliveryTrade(item.path("COP_DELIV_QTY").asLong(0L));
			stock.setDeliveryPercentage(item.path("COP_DELIV_PERC").asDouble(0.0));
			stock.setVwap(item.path("VWAP").asDouble(0.0));

			produceConsumePdv(stock, lastTicker);
			stockList.add(stock);
		}
		return stockList;
	}

	private void produceConsumePdv(PriceDeliveryVolume stock, AtomicReference<String> lastTicker) {
		// Create topic for consumption
		if (lastTicker.get() == null) {
			this.kafkaUtils.createTopic(stock.getTicker());
			lastTicker.set(stock.getTicker());

			// Consume the PDV data
			this.kafkaUtils.consumePDVforATR(lastTicker.get());
		}

		if (!lastTicker.get().equals(stock.getTicker())) {
			this.kafkaUtils.createTopic(stock.getTicker());
			lastTicker.set(stock.getTicker());

			// Consume the PDV data
			this.kafkaUtils.consumePDVforATR(lastTicker.get());
		}

		// Produce data for consumption
		this.kafkaUtils.produceToStockTopic(stock.getTicker(), stock);
	}

	// Helper function to parse dates safely
	private LocalDate parseDate(String dateStr) {
		try {
			return LocalDate.parse(dateStr, NSE_DATE_FORMATTER);
		} catch (Exception e) {
			return null; // Or LocalDate.now() or throw, based on your requirement
		}
	}
}