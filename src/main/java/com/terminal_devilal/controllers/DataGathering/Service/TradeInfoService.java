package com.terminal_devilal.controllers.DataGathering.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.controllers.DataGathering.DAO.TradeInfoRepository;
import com.terminal_devilal.controllers.DataGathering.Model.TradeInfo;

import java.time.LocalDate;
import java.util.Optional;

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
            JsonNode tradeInfoNode = rootNode
                    .path("marketDeptOrderBook")
                    .path("tradeInfo");

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
            tradeInfo.setCmDailyVolatility(getDouble(tradeInfoNode, "cmDailyVolatility"));
            tradeInfo.setCmAnnualVolatility(getDouble(tradeInfoNode, "cmAnnualVolatility"));


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
    
    public void saveTradeInfo(TradeInfo data) {
    	this.repository.save(data);
    }
}

