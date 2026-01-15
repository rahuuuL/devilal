package com.terminal_devilal.core_processes.sync_data.service;

import java.util.LinkedList;
import java.util.Optional;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.JsonNode;
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
	public void persistAll(String ticker, TreeSet<PriceDeliveryVolumeEntity> pdvList,
			Optional<com.terminal_devilal.business_tools.trade_info.entities.TradeInfo> tradeInfo,
			JsonNode pdvResponse) {

		// 1️⃣ PDV inserts
		if (!pdvList.isEmpty()) {
			priceDeliveryVolumeService.saveAllPdvList(new LinkedList<>(pdvList));

			// 2️⃣ Update last processed date
			dataFetchHistoryService.updateLastDateForPdvt(ticker, pdvList.last().getDate());
		}

		// 3️⃣ Trade info insert/update
		tradeInfo.ifPresent(tradeInfoService::saveTradeInfo);

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				produceKafkaMessage(pdvResponse);
			}
		});
	}

	private void produceKafkaMessage(JsonNode node) {
		JsonNode dataArray = node.path("data");
		if (!dataArray.isArray())
			return;

		for (JsonNode item : dataArray) {
			kafkaProducerService.sendMessage(item.toPrettyString());
		}
	}
}
