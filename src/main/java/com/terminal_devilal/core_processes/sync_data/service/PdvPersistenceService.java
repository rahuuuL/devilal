package com.terminal_devilal.core_processes.sync_data.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.business_tools.trade_info.entities.TradeInfo;
import com.terminal_devilal.business_tools.trade_info.service.TradeInfoService;
import com.terminal_devilal.configurations.kakfa.KafkaProducerService;
import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.pipeline.audit.PipelineAuditService;
import com.terminal_devilal.pipeline.audit.PipelineAuditStage;
import com.terminal_devilal.pipeline.audit.PipelineTickerContext;

import jakarta.transaction.Transactional;

@Service
public class PdvPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PdvPersistenceService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    private final PriceDeliveryVolumeService priceDeliveryVolumeService;
    private final DataFetchHistoryService dataFetchHistoryService;
    private final TradeInfoService tradeInfoService;
    private final KafkaProducerService kafkaProducerService;
    private final PipelineAuditService pipelineAuditService;

    public PdvPersistenceService(PriceDeliveryVolumeService priceDeliveryVolumeService,
            DataFetchHistoryService dataFetchHistoryService, TradeInfoService tradeInfoService,
            KafkaProducerService kafkaProducerService, PipelineAuditService pipelineAuditService) {
        this.priceDeliveryVolumeService = priceDeliveryVolumeService;
        this.dataFetchHistoryService = dataFetchHistoryService;
        this.tradeInfoService = tradeInfoService;
        this.kafkaProducerService = kafkaProducerService;
        this.pipelineAuditService = pipelineAuditService;
    }

    @Transactional
    public void persistAll(String ticker, TreeSet<PriceDeliveryVolumeEntity> pdvList, Optional<TradeInfo> tradeInfo,
            JsonNode pdvResponse, PipelineTickerContext tickerContext) {

        if (!pdvList.isEmpty()) {
            pipelineAuditService.logStageStart(tickerContext, PipelineAuditStage.PDVT_SAVE, "Persisting PDVT records");
            if (tradeInfo.isPresent()) {
                tradeInfoService.saveTradeInfo(tradeInfo.get(), tickerContext);
            }
            priceDeliveryVolumeService.saveAllPdvList(new LinkedList<>(pdvList), tickerContext);
            dataFetchHistoryService.updateLastDateForPdvt(ticker, pdvList.last().getDate(), tickerContext);
            tickerContext.getMetrics().incrementPdvtSaved();
            pipelineAuditService.logStageSuccess(tickerContext, PipelineAuditStage.PDVT_SAVE,
                    pdvList.size(), pdvList.first().getDate().toString(), pdvList.last().getDate().toString(), null,
                    "PDVT records saved");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                produceKafkaMessage(ticker, pdvResponse, tickerContext);
            }
        });
    }

    private void produceKafkaMessage(String ticker, JsonNode node, PipelineTickerContext tickerContext) {
        JsonNode dataArray = node.path("data");
        if (!dataArray.isArray()) {
            return;
        }

        List<JsonNode> sorted = new ArrayList<>();
        dataArray.forEach(sorted::add);
        sorted.sort(Comparator.comparing(item -> parseTimestamp(item.path("CH_TIMESTAMP").asText())));

        for (JsonNode item : sorted) {
            kafkaProducerService.sendMessage("pdv-data", ticker, item.toPrettyString(), tickerContext);
        }
    }

    LocalDate parseTimestamp(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return LocalDate.of(1970, 1, 1);
        }

        String value = rawValue.trim();

        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // Try ISO-8601 / ISO date-time values such as 2021-03-30T18:30:00.000Z
        }

        try {
            return Instant.parse(value).atOffset(ZoneOffset.UTC).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Try offset-aware date-time values
        }

        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // Fall back to a deterministic default so the pipeline keeps moving
        }

        log.warn("Unable to parse PDV timestamp '{}'; using fallback date 1970-01-01", value);
        return LocalDate.of(1970, 1, 1);
    }
}
