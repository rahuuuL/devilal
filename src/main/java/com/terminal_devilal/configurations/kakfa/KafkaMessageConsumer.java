package com.terminal_devilal.configurations.kakfa;

import java.util.List;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terminal_devilal.indicators.atr.service.AverageTrueRangeService;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeUtility;
import com.terminal_devilal.indicators.rsi.service.RSIService;
import com.terminal_devilal.indicators.vwap.service.VWAPService;

@Service
public class KafkaMessageConsumer {

	private final AverageTrueRangeService averageTrueRangeService;
	private final RSIService rsiService;
	private final VWAPService vwapService;
	private final PriceDeliveryVolumeUtility priceDeliveryVolumeUtility;

	// Singleton ObjectMapper — thread-safe for concurrent reads
	private static final ObjectMapper mapper = new ObjectMapper();

	public KafkaMessageConsumer(AverageTrueRangeService averageTrueRangeService, RSIService rsiService,
			VWAPService vwapService, PriceDeliveryVolumeUtility priceDeliveryVolumeUtility) {
		this.averageTrueRangeService = averageTrueRangeService;
		this.rsiService = rsiService;
		this.vwapService = vwapService;
		this.priceDeliveryVolumeUtility = priceDeliveryVolumeUtility;
	}

	private static final int FLUSH_EVERY = 500;

	@KafkaListener(topics = "pdv-data", groupId = "devilal-group", concurrency = "8", containerFactory = "batchFactory")
	public void listen(List<ConsumerRecord<String, String>> records) {

		int processedCount = 0;

		for (ConsumerRecord<String, String> record : records) {

			Optional<PriceDeliveryVolumeEntity> optionalEntity = parseEntity(record.value());
			if (optionalEntity.isEmpty())
				continue;

			PriceDeliveryVolumeEntity entity = optionalEntity.get();

			try {
				averageTrueRangeService.processATR(entity);
			} catch (Exception e) {
				System.err.printf("[ATR] error ticker=%s: %s%n", entity.getTicker(), e.getMessage());
			}

			try {
				rsiService.processRSI(entity);
			} catch (Exception e) {
				System.err.printf("[RSI] error ticker=%s: %s%n", entity.getTicker(), e.getMessage());
			}

			try {
				vwapService.processVwap(entity);
			} catch (Exception e) {
				System.err.printf("[VWAP] error ticker=%s: %s%n", entity.getTicker(), e.getMessage());
			}

			processedCount++;

			if (processedCount % FLUSH_EVERY == 0) {
				averageTrueRangeService.flushBuffer();
				rsiService.flushBuffer();
				vwapService.flushBuffer();
			}
		}

		// Flush remaining records that didn't fill a complete chunk
		averageTrueRangeService.flushBuffer();
		rsiService.flushBuffer();
		vwapService.flushBuffer();
	}

	// Parses the raw Kafka message string directly into a
	// PriceDeliveryVolumeEntity.
	// If your PriceDeliveryVolumeUtility requires a JsonNode, swap the
	// implementation
	// to parse to JsonNode first and then call utility.parseStockData(node).
	private Optional<PriceDeliveryVolumeEntity> parseEntity(String data) {
		try {
			JsonNode node = mapper.readTree(data);
			return Optional.of(priceDeliveryVolumeUtility.parseStockData(node));
		} catch (JsonProcessingException e) {
			System.err.printf("[CONSUMER] JSON parse error: %s%n", e.getMessage());
			return Optional.empty();
		}
	}
}