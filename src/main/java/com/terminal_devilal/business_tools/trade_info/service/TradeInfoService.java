package com.terminal_devilal.business_tools.trade_info.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.business_tools.trade_info.entities.TradeInfo;
import com.terminal_devilal.business_tools.trade_info.repository.TradeInfoRepository;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class TradeInfoService {

	private final TradeInfoRepository repository;

	public TradeInfoService(TradeInfoRepository repository) {
		super();
		this.repository = repository;
	}

	public Optional<TradeInfo> parseTradeInfo(JsonNode rootNode, String ticker, LocalDate date) {
		try {
			JsonNode tradeInfoNode = rootNode.path("marketDeptOrderBook").path("tradeInfo");

			if (tradeInfoNode.isMissingNode() || tradeInfoNode.isNull()) {
				System.err.println("Missing 'tradeInfo' node for ticker: " + ticker);
				return Optional.empty();
			}

			TradeInfo tradeInfo = new TradeInfo();
			tradeInfo.setTicker(ticker);
			tradeInfo.setDate(date);

			tradeInfo.setTotalTradedVolume(getDouble(tradeInfoNode, "totalTradedVolume"));
			tradeInfo.setTotalTradedValue(getDouble(tradeInfoNode, "totalTradedValue"));
			tradeInfo.setTotalMarketCap(getDouble(tradeInfoNode, "totalMarketCap"));
			tradeInfo.setFfmc(getDouble(tradeInfoNode, "ffmc"));
			tradeInfo.setImpactCost(getDouble(tradeInfoNode, "impactCost"));
			tradeInfo.setCmDailyVolatility(parseDoubleSafe(getString(tradeInfoNode, "cmDailyVolatility")));
			tradeInfo.setCmAnnualVolatility(parseDoubleSafe(getString(tradeInfoNode, "cmAnnualVolatility")));

			return Optional.of(tradeInfo);
		} catch (Exception e) {
			System.err.println("Error parsing tradeInfo for ticker " + ticker + ": " + e.getMessage());
			return Optional.empty();
		}
	}

	private static Double getDouble(JsonNode node, String key) {
		if (node.has(key) && node.get(key).isNumber()) {
			return node.get(key).asDouble();
		}
		System.err.println("Missing or invalid numeric key: " + key);
		return null;
	}

	private double parseDoubleSafe(String value) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return 0.0; // or Double.NaN, or throw, depending on your needs
		}
	}

	private static String getString(JsonNode node, String key) {
		if (node.has(key)) {
			return node.get(key).asText();
		}
		System.err.println("Missing or invalid string key: " + key);
		return null;
	}

	public void saveTradeInfo(TradeInfo data) {
		this.repository.save(data);
	}

	public Map<String, TradeInfo> getTradeInfoData() {
		Map<String, TradeInfo> tradeInfoMap = this.repository.findLatestTradeInfoPerTicker().stream()
				.collect(Collectors.toMap(TradeInfo::getTicker, Function.identity()));
		return tradeInfoMap;
	}
}
