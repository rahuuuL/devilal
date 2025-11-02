package com.terminal_devilal.configurations.kakfa;

import java.util.Optional;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terminal_devilal.indicators.atr.service.AverageTrueRangeService;
import com.terminal_devilal.indicators.rsi.service.RSIService;
import com.terminal_devilal.indicators.vwap.service.VWAPService;

@Service
public class KafkaMessageConsumer {

	private final Executor executor;
	private final AverageTrueRangeService averageTrueRangeService;
	private final RSIService rsiService;
	private final VWAPService vwapService;

	private final ObjectMapper mapper = new ObjectMapper();

	public KafkaMessageConsumer(AverageTrueRangeService averageTrueRangeService, RSIService rsiService,
			VWAPService vwapService, @Qualifier("kafkaProcessingExecutor") Executor executor) {
		super();
		this.executor = executor;
		this.averageTrueRangeService = averageTrueRangeService;
		this.rsiService = rsiService;
		this.vwapService = vwapService;
	}

	@KafkaListener(topics = "pdv-data", groupId = "devilal-group", concurrency = "32")
	public void listen(String pdv) {
		getJsonNode(pdv).ifPresent(jsonNode -> {
			executor.execute(() -> {
				// Process ATR
				this.averageTrueRangeService.processATR(jsonNode);

				// Process RSI
				this.rsiService.processRSI(jsonNode);

				// Process RSI
				this.vwapService.processVwap(jsonNode);
			});
		});
	}

	private Optional<JsonNode> getJsonNode(String data) {
		try {
			return Optional.of(mapper.readTree(data));
		} catch (JsonProcessingException e) {
			System.out.println("Data parse exception in ATR for data : " + data.toString());
			e.printStackTrace();
			return Optional.empty();

		}
	}
}
