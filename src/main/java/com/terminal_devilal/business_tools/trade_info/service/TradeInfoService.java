package com.terminal_devilal.business_tools.trade_info.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.business_tools.trade_info.entity.TradeInfo;
import com.terminal_devilal.business_tools.trade_info.repository.TradeInfoRepository;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.core_processes.pipeline.audit.PipelineTickerContext;

@Service
public class TradeInfoService {

    private static final Logger log = LoggerFactory.getLogger(TradeInfoService.class);

    private final TradeInfoRepository repository;
    private final PipelineAuditService pipelineAuditService;

    public TradeInfoService(TradeInfoRepository repository, PipelineAuditService pipelineAuditService) {
        this.repository = repository;
        this.pipelineAuditService = pipelineAuditService;
    }

    public Optional<TradeInfo> parseTradeInfo(JsonNode rootNode, String ticker, LocalDate date) {
        return parseTradeInfo(rootNode, ticker, date, null);
    }

    public Optional<TradeInfo> parseTradeInfo(JsonNode rootNode, String ticker, LocalDate date, PipelineTickerContext tickerContext) {
        try {
            JsonNode equityArray = rootNode.path("equityResponse");
            JsonNode firstRecord = equityArray.isArray() && equityArray.size() > 0 ? equityArray.get(0) : null;
            JsonNode tradeInfoNode = firstRecord == null ? null : firstRecord.path("tradeInfo");
            JsonNode priceInfoNode = firstRecord == null ? null : firstRecord.path("priceInfo");

            if (tradeInfoNode == null || tradeInfoNode.isMissingNode() || tradeInfoNode.isNull()) {
                log.warn("Missing tradeInfo node for ticker {}", ticker);
                if (tickerContext != null) {
                    pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.TRADEINFO_SAVE,
                            "TradeInfo missing from API payload", new IllegalStateException("tradeInfo missing"));
                }
                return Optional.empty();
            }

            TradeInfo tradeInfo = new TradeInfo();
            tradeInfo.setTicker(ticker);
            tradeInfo.setDate(date);

            tradeInfo.setTotalTradedVolume(getDoubleOrDefault(tradeInfoNode, "totalTradedVolume", 0.0));
            tradeInfo.setTotalTradedValue(getDoubleOrDefault(tradeInfoNode, "totalTradedValue", 0.0));
            tradeInfo.setTotalMarketCap(getDoubleOrDefault(tradeInfoNode, "totalMarketCap", 0.0));
            tradeInfo.setFfmc(getDoubleOrDefault(tradeInfoNode, "ffmc", 0.0));
            tradeInfo.setImpactCost(getDoubleOrDefault(tradeInfoNode, "impactCost", 0.0));
            tradeInfo.setCmDailyVolatility(parseDoubleSafe(getStringOrNull(priceInfoNode, "cmDailyVolatility")));
            tradeInfo.setCmAnnualVolatility(parseDoubleSafe(getStringOrNull(priceInfoNode, "cmAnnualVolatility")));

            if (tickerContext != null) {
                pipelineAuditService.logStageSuccess(tickerContext, PipelineAuditStage.TRADEINFO_SAVE, 1, date.toString(), date.toString(), null, "TradeInfo parsed");
            }
            return Optional.of(tradeInfo);
        } catch (Exception e) {
            log.error("Error parsing tradeInfo for ticker {}", ticker, e);
            if (tickerContext != null) {
                pipelineAuditService.logStageFailure(tickerContext, PipelineAuditStage.TRADEINFO_SAVE,
                        "TradeInfo parsing failed", e);
            }
            return Optional.empty();
        }
    }

    private static double getDoubleOrDefault(JsonNode node, String key, double defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        if (node.has(key) && node.get(key).isNumber()) {
            return node.get(key).asDouble();
        }
        log.warn("Missing or invalid numeric key: {}", key);
        return defaultValue;
    }

    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String getStringOrNull(JsonNode node, String key) {
        if (node == null) {
            return null;
        }
        if (node.has(key)) {
            return node.get(key).asText();
        }
        log.warn("Missing or invalid string key: {}", key);
        return null;
    }

    public void saveTradeInfo(TradeInfo data) {
        this.repository.save(data);
    }

    public void saveTradeInfo(TradeInfo data, PipelineTickerContext tickerContext) {
        this.repository.save(data);
        if (tickerContext != null) {
            tickerContext.getMetrics().incrementPdvtSaved();
            pipelineAuditService.logStageSuccess(tickerContext, PipelineAuditStage.TRADEINFO_SAVE, 1,
                    data.getDate().toString(), data.getDate().toString(), null, "TradeInfo persisted");
        }
    }

    public Map<String, TradeInfo> getTradeInfoData() {
        Map<String, TradeInfo> tradeInfoMap = this.repository.findLatestTradeInfoPerTicker().stream()
                .collect(Collectors.toMap(TradeInfo::getTicker, Function.identity()));
        return tradeInfoMap;
    }
}
