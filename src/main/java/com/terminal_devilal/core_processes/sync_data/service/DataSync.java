package com.terminal_devilal.core_processes.sync_data.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.business_tools.trade_info.entities.TradeInfo;
import com.terminal_devilal.business_tools.trade_info.service.TradeInfoService;
import com.terminal_devilal.configurations.kakfa.KafkaProducerService;
import com.terminal_devilal.core_processes.dfht.entities.DataFetchEntity;
import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.utils.nse.FetchNSEAPI;

@Service
public class DataSync {

	// Setup date formatter to match API format like "25-05-2025"
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	// Define number of threads to run in parallel (max API calls at a time)
	private final int THREAD_POOL_SIZE = 50;

	private final int BATCH_LIMIT = 99;

	// 5 seconds wait after max API calls
	private final int SLEEP_TIME_MS = 10000;

	private final DataFetchHistoryService dataFetchHistoryService;

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	private final TradeInfoService tradeInfoService;

	private final FetchNSEAPI fetchNSEAPI;

	private final KafkaProducerService kafkaProducerService;

	public DataSync(DataFetchHistoryService dataFetchHistoryService,
			PriceDeliveryVolumeService priceDeliveryVolumeService, TradeInfoService tradeInfoService,
			FetchNSEAPI fetchNSEAPI, KafkaProducerService kafkaProducerService) {
		super();
		this.dataFetchHistoryService = dataFetchHistoryService;
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
		this.tradeInfoService = tradeInfoService;
		this.fetchNSEAPI = fetchNSEAPI;
		this.kafkaProducerService = kafkaProducerService;
	}

	public void processPdvDataTillDate() {
		List<DataFetchEntity> processedDates = dataFetchHistoryService.getProcessedDatesForTickers();
		processedDates = processedDates.stream().filter(data -> data.getPdvtLastDate().isBefore(LocalDate.now()))
				.collect(Collectors.toList());
		int total = processedDates.size();

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		AtomicInteger counter = new AtomicInteger(0);

		for (int i = 0; i < total; i++) {
			DataFetchEntity data = processedDates.get(i);

			// Pause every BATCH API calls
			if (i > 0 && i % BATCH_LIMIT == 0) {
				System.out
						.println("Submitted " + i + " API calls. Pausing for " + SLEEP_TIME_MS / 1000 + " seconds...");
				try {
					Thread.sleep(SLEEP_TIME_MS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.err.println("Sleep interrupted: " + e.getMessage());
					break;
				}
			}

			executor.submit(() -> {
				try {
					// Make URL
					String toDate = getToDate(data.getPdvtLastDate().format(FORMATTER));
					String fromDate = getFromDate(toDate, data.getPdvtLastDate().format(FORMATTER));

					String pdvUrl = this.fetchNSEAPI.buildPDVUrl(fromDate, toDate, data.getTicker());
					String tradeInfoUrl = this.fetchNSEAPI.buildTradeInfoUrl(data.getTicker());

					// Fetch Data PDV Data
					JsonNode pdvResponse = this.fetchNSEAPI.NSEAPICall(pdvUrl);

					TreeSet<PriceDeliveryVolumeEntity> pdvList = this.priceDeliveryVolumeService
							.parseStockData(pdvResponse, data.getTicker());
					FetchResult fetchResult = new FetchResult(toDate);
					if (pdvList.isEmpty()) {
						fetchResult = fetchTillDate(fromDate, toDate, data.getTicker());
						try {
							Thread.sleep(SLEEP_TIME_MS);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							System.err.println("Sleep interrupted: " + e.getMessage());
						}
					}

					// Fetch Data Trade Info Data and Save the data
					JsonNode tradeInfoResponse = this.fetchNSEAPI.NSEAPICall(tradeInfoUrl);
					Optional<TradeInfo> info = this.tradeInfoService.parseTradeInfo(tradeInfoResponse, data.getTicker(),
							LocalDate.now());
					info.ifPresent(tradeInfo -> this.tradeInfoService.saveTradeInfo(tradeInfo));

					// Save the Data
					if (fetchResult.hasData() && pdvList.isEmpty()) {
						priceDeliveryVolumeService.saveAllPdvList(new LinkedList<>(fetchResult.getData()));

						// update last pdvt date
						dataFetchHistoryService.updateLastDateForPdvt(data.getTicker(),
								LocalDate.parse(fetchResult.getToDate(), FORMATTER));
					} else {
						priceDeliveryVolumeService.saveAllPdvList(new LinkedList<>(pdvList));

						// update last pdvt date
						dataFetchHistoryService.updateLastDateForPdvt(data.getTicker(), pdvList.last().getDate());
					}

					// produce data to kafka for further processing
					this.produceKafkaMessage(pdvResponse);

				} catch (IOException | InterruptedException e) {
					System.err.println("Error processing " + data.getTicker() + ": " + e.getMessage());
				}

				// Track and print progress
				int current = counter.incrementAndGet();
				double progress = (100.0 * current) / total;
				System.out.println("Processed ticker : " + data.getTicker());
				System.out.printf("Progress: %.2f%% (%d/%d completed)\n", progress, current, total);
			});
		}

		executor.shutdown();
	}

	private void produceKafkaMessage(JsonNode node) {
		JsonNode dataArray = node.path("data");

		if (!dataArray.isArray()) {
			System.out.print("Empty data" + node.toPrettyString());
			return;
		}

		for (JsonNode item : dataArray) {
			this.kafkaProducerService.sendMessage(item.toPrettyString());
		}
	}

	private String getToDate(String fromDateStr) {
		LocalDate fromDate = LocalDate.parse(fromDateStr, FORMATTER);
		LocalDate today = LocalDate.now();

		// if fromDate is more than 3 months older than today
		if (fromDate.plusMonths(3).isBefore(today)) {
			// return fromDate + 3 months
			return fromDate.plusMonths(3).format(FORMATTER);
		} else {
			// else return today's date
			return today.format(FORMATTER);
		}
	}

	private String getFromDate(String toDate, String pdvtLastDate) {
		LocalDate fromDate = LocalDate.parse(pdvtLastDate, FORMATTER);

		if (LocalDate.parse(toDate, FORMATTER).minusDays(1).isEqual(fromDate)) {
			return toDate;
		}
		return pdvtLastDate;
	}

	private FetchResult fetchTillDate(String fromDate, String toDate, String ticker) {
		if (LocalDate.parse(toDate, FORMATTER).isAfter(LocalDate.now())) {
			return new FetchResult(LocalDate.now().format(FORMATTER));
		}
		fromDate = toDate;
		toDate = LocalDate.parse(toDate, FORMATTER).plusMonths(3).format(FORMATTER);
		String pdvUrl = this.fetchNSEAPI.buildPDVUrl(fromDate, toDate, ticker);

		// Fetch Data PDV Data
		try {
			JsonNode pdvResponse = this.fetchNSEAPI.NSEAPICall(pdvUrl);

			TreeSet<PriceDeliveryVolumeEntity> pdvList = this.priceDeliveryVolumeService.parseStockData(pdvResponse,
					ticker);
			if (pdvList.isEmpty()) {
				return fetchTillDate(fromDate, toDate, ticker);
			} else {
				return new FetchResult(pdvList);
			}
		} catch (IOException | InterruptedException e) {
			System.err.println("Error processing " + ticker + ": " + e.getMessage());
			e.printStackTrace();
			return new FetchResult(LocalDate.now().format(FORMATTER));
		}
	}

	public class FetchResult {
		private TreeSet<PriceDeliveryVolumeEntity> data;
		private String toDate;

		public FetchResult(TreeSet<PriceDeliveryVolumeEntity> data) {
			this.data = data;
		}

		public FetchResult(String toDate) {
			this.toDate = toDate;
		}

		public boolean hasData() {
			return data != null && !data.isEmpty();
		}

		public TreeSet<PriceDeliveryVolumeEntity> getData() {
			return data;
		}

		public String getToDate() {
			return toDate;
		}
	}

}
