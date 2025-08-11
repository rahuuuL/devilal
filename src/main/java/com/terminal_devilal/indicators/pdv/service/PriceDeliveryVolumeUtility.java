package com.terminal_devilal.indicators.pdv.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.exception.DateParserException;

@Service
public class PriceDeliveryVolumeUtility {
	private final DateTimeFormatter NSE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

	// Helper function to parse dates safely
	public LocalDate parseDate(String dateStr) {
		try {
			return LocalDate.parse(dateStr, NSE_DATE_FORMATTER);
		} catch (Exception e) {
			throw new DateParserException("Error occured in processing date : " + dateStr, e);
		}
	}

	public PriceDeliveryVolumeEntity parseStockData(JsonNode node) {

		PriceDeliveryVolumeEntity stock = new PriceDeliveryVolumeEntity();
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

}
