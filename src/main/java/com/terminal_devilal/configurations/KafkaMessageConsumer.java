package com.terminal_devilal.configurations;

import java.util.Optional;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terminal_devilal.controllers.DataGathering.Service.AverageTrueRangeService;
import com.terminal_devilal.controllers.DataGathering.Service.RSIService;
import com.terminal_devilal.controllers.DataGathering.Service.VWAPService;

@Service
public class KafkaMessageConsumer {

	private final AverageTrueRangeService averageTrueRangeService;
	private final RSIService rsiService;
	private final VWAPService vwapService;

	private final ObjectMapper mapper = new ObjectMapper();

	public KafkaMessageConsumer(AverageTrueRangeService averageTrueRangeService, RSIService rsiService,
			VWAPService vwapService) {
		super();
		this.averageTrueRangeService = averageTrueRangeService;
		this.rsiService = rsiService;
		this.vwapService = vwapService;
	}

	@KafkaListener(topics = "pdv-data", groupId = "devilal-group", concurrency = "4")
	public void listen(String pdv) {
		getJsonNode(pdv).ifPresent(jsonNode -> {
			
			// Process ATR
			this.averageTrueRangeService.processATR(jsonNode);
			
			// Process RSI
			this.rsiService.processRSI(jsonNode);
			
			// Process RSI
			this.vwapService.processVwap(jsonNode);
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
