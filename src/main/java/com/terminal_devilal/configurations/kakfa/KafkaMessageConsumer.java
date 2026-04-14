package com.terminal_devilal.configurations.kakfa;

import java.util.Optional;
import java.util.concurrent.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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

	private ExecutorService executor;

	private final AverageTrueRangeService averageTrueRangeService;
	private final RSIService rsiService;
	private final VWAPService vwapService;

	private static final ObjectMapper mapper = new ObjectMapper();

	public KafkaMessageConsumer(AverageTrueRangeService averageTrueRangeService, RSIService rsiService,
			VWAPService vwapService) {
		this.averageTrueRangeService = averageTrueRangeService;
		this.rsiService = rsiService;
		this.vwapService = vwapService;
	}

	// 🔥 Create executor inside same class (no separate config)
	@PostConstruct
	public void init() {
		int threads = Runtime.getRuntime().availableProcessors();

		this.executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(10000), // bounded queue
				new ThreadPoolExecutor.CallerRunsPolicy() // backpressure
		);
	}

	@PreDestroy
	public void shutdown() {
		executor.shutdown();
	}

	// 🔥 Kafka listener (kept 32 as you asked)
	@KafkaListener(topics = "pdv-data", groupId = "devilal-group", concurrency = "32")
	public void listen(String pdv) {

		Optional<JsonNode> optionalNode = getJsonNode(pdv);
		if (optionalNode.isEmpty())
			return;

		JsonNode jsonNode = optionalNode.get();

		// 🔥 TRUE PARALLEL PROCESSING (3 independent tasks)

		executor.submit(() -> {
			try {
				averageTrueRangeService.processATR(jsonNode);
			} catch (Exception e) {
				System.err.println("ATR error: " + e.getMessage());
			}
		});

		executor.submit(() -> {
			try {
				rsiService.processRSI(jsonNode);
			} catch (Exception e) {
				System.err.println("RSI error: " + e.getMessage());
			}
		});

		executor.submit(() -> {
			try {
				vwapService.processVwap(jsonNode);
			} catch (Exception e) {
				System.err.println("VWAP error: " + e.getMessage());
			}
		});
	}

	// 🔧 JSON parsing (optimized with singleton ObjectMapper)
	private Optional<JsonNode> getJsonNode(String data) {
		try {
			return Optional.of(mapper.readTree(data));
		} catch (JsonProcessingException e) {
			System.err.println("JSON parse error for data: " + data);
			return Optional.empty();
		}
	}
}