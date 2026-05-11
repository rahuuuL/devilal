package com.terminal_devilal.core_processes.sync_data.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

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

import jakarta.transaction.Transactional;

@Service
public class PdvPersistenceService {

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;
	private final DataFetchHistoryService dataFetchHistoryService;
	@SuppressWarnings("unused")
	private final TradeInfoService tradeInfoService;
	private final KafkaProducerService kafkaProducerService;

	public PdvPersistenceService(PriceDeliveryVolumeService priceDeliveryVolumeService,
			DataFetchHistoryService dataFetchHistoryService, TradeInfoService tradeInfoService,
			KafkaProducerService kafkaProducerService) {
		super();
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
		this.dataFetchHistoryService = dataFetchHistoryService;
		this.tradeInfoService = tradeInfoService;
		this.kafkaProducerService = kafkaProducerService;
	}

	/**
	 * SINGLE transactional boundary for ALL DB writes
	 */
	@Transactional
	public void persistAll(String ticker, TreeSet<PriceDeliveryVolumeEntity> pdvList, Optional<TradeInfo> tradeInfo,
			JsonNode pdvResponse) {

		if (!pdvList.isEmpty()) {
			if (tradeInfo.isPresent()) {
				tradeInfoService.saveTradeInfo(tradeInfo.get());
			}
			priceDeliveryVolumeService.saveAllPdvList(new LinkedList<>(pdvList));
			dataFetchHistoryService.updateLastDateForPdvt(ticker, pdvList.last().getDate());
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				produceKafkaMessage(ticker, pdvResponse); // pass ticker through
			}
		});
	}

	private void produceKafkaMessage(String ticker, JsonNode node) {
		JsonNode dataArray = node.path("data");
		if (!dataArray.isArray())
			return;

		// Sort by date ascending before producing — never trust API order
		List<JsonNode> sorted = new ArrayList<>();
		dataArray.forEach(sorted::add);
		sorted.sort(Comparator.comparing(item -> item.path("CH_TIMESTAMP").asText())); // use your actual date field
																						// name

		for (JsonNode item : sorted) {
			// Ticker as key — guarantees all messages for same ticker
			// land on same partition in the order you send them
			kafkaProducerService.sendMessage("pdv-data", ticker, item.toPrettyString());
		}
	}
}
