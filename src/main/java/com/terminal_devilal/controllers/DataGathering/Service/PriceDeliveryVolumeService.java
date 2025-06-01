package com.terminal_devilal.controllers.DataGathering.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.configurations.KafkaProducerService;
import com.terminal_devilal.controllers.DataGathering.DAO.PriceDeliveryVolumeDAO;
import com.terminal_devilal.controllers.DataGathering.Exception.DateParserException;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;

@Service
public class PriceDeliveryVolumeService {

	private final PriceDeliveryVolumeDAO repository;

	private final KafkaProducerService kafkaProducerService;

	public PriceDeliveryVolumeService(PriceDeliveryVolumeDAO repository, KafkaProducerService kafkaProducerService) {
		super();
		this.repository = repository;
		this.kafkaProducerService = kafkaProducerService;
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
	public TreeSet<PriceDeliveryVolume> parseStockDataAndProduce(JsonNode node, String ticker) {

		TreeSet<PriceDeliveryVolume> stockList = new TreeSet<PriceDeliveryVolume>(
				Comparator.comparing(PriceDeliveryVolume::getDate));
		JsonNode dataArray = node.path("data");

		if (!dataArray.isArray()) {
			System.out.print("Empty date" + node.toPrettyString());
			return stockList; // return empty if "data" is not an array
		}

		for (JsonNode item : dataArray) {
			PriceDeliveryVolume stock = parseStockData(item);
			stock.setTicker(ticker);
			stockList.add(stock);
			this.kafkaProducerService.sendMessage(item.toPrettyString());

		}
		return stockList;
	}

	// Mapper Utility function
	public PriceDeliveryVolume parseStockData(JsonNode node) {

		PriceDeliveryVolume stock = new PriceDeliveryVolume();
		stock.setTicker(node.path("CH_SYMBOL").asText(""));
		stock.setDate(parseDate(node.path("mTIMESTAMP").asText("")));
		stock.setHigh(node.path("CH_TRADE_HIGH_PRICE").asDouble(0.0));
		stock.setLow(node.path("CH_TRADE_LOW_PRICE").asDouble(0.0));
		stock.setOpen(node.path("CH_OPENING_PRICE").asDouble(0.0));
		stock.setClose(node.path("CH_CLOSING_PRICE").asDouble(0.0));
		stock.setLastTradeValue(node.path("CH_LAST_TRADED_PRICE").asDouble(0.0));
		stock.setPrevoiusClosePrice(node.path("CH_PREVIOUS_CLS_PRICE").asDouble(0.0));
		stock.setVolume(node.path("CH_TOT_TRADED_QTY").asLong(0L));
		stock.setValue(node.path("CH_TOT_TRADED_VAL").asDouble(0.0));
		stock.setTrades(node.path("CH_TOTAL_TRADES").asInt(0));
		stock.setDeliveryTrade(node.path("COP_DELIV_QTY").asLong(0L));
		stock.setDeliveryPercentage(node.path("COP_DELIV_PERC").asDouble(0.0));
		stock.setVwap(node.path("VWAP").asDouble(0.0));

		return stock;
	}

	// Helper function to parse dates safely
	public LocalDate parseDate(String dateStr) {
		try {
			return LocalDate.parse(dateStr, NSE_DATE_FORMATTER);
		} catch (Exception e) {
			throw new DateParserException("Error occured in processing date : " + dateStr, e);
		}
	}
}