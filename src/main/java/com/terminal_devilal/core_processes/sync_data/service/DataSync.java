package com.terminal_devilal.core_processes.sync_data.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.business_tools.trade_info.entities.TradeInfo;
import com.terminal_devilal.core_processes.dfht.entities.DataFetchEntity;
import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.utils.nse.FetchNSEAPI;

@Service
public class DataSync {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	private static final int API_THREADS = 8; // I/O threads
	private static final int PROCESS_THREADS = Runtime.getRuntime().availableProcessors(); // CPU threads
	private static final int QUEUE_SIZE = 10000;

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final DataFetchHistoryService dataFetchHistoryService;
	private final PriceDeliveryVolumeService priceDeliveryVolumeService;
	private final FetchNSEAPI fetchNSEAPI;
	private final PdvPersistenceService pdvPersistenceService;

	public DataSync(DataFetchHistoryService dataFetchHistoryService,
			PriceDeliveryVolumeService priceDeliveryVolumeService, FetchNSEAPI fetchNSEAPI,
			PdvPersistenceService pdvPersistenceService) {

		this.dataFetchHistoryService = dataFetchHistoryService;
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
		this.fetchNSEAPI = fetchNSEAPI;
		this.pdvPersistenceService = pdvPersistenceService;
	}

	public void processPdvDataTillDate() {

		if (!running.compareAndSet(false, true)) {
			System.out.println("⚠️ PDV sync already running. Skipping.");
			return;
		}

		// 🔥 Pipeline queue
		BlockingQueue<WorkItem> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);

		ExecutorService apiExecutor = Executors.newFixedThreadPool(API_THREADS);
		ExecutorService processExecutor = Executors.newFixedThreadPool(PROCESS_THREADS);

		try {
			List<DataFetchEntity> records = dataFetchHistoryService.getProcessedDatesForTickers().stream()
					.filter(d -> d.getPdvtLastDate().isBefore(LocalDate.now())).toList();

			int total = records.size();
			AtomicInteger completed = new AtomicInteger();

			System.out.println("Starting PDV sync for " + total + " tickers");

			// 🔥 Stage 2: Processing workers
			for (int i = 0; i < PROCESS_THREADS; i++) {
				processExecutor.submit(() -> {
					while (true) {
						try {
							WorkItem item = queue.poll(10, TimeUnit.SECONDS);
							if (item == null)
								break;

							processAndPersist(item);

							int done = completed.incrementAndGet();
							System.out.printf("Progress: %.2f%% (%d/%d)%n", (100.0 * done) / total, done, total);

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}

			// 🔥 Stage 1: API fetch
			for (DataFetchEntity data : records) {
				apiExecutor.submit(() -> {
					try {
//						String from = data.getPdvtLastDate().format(FORMATTER);
//						String to = calculateToDate(from);
//
//						JsonNode response = fetchNSEAPI.NSEAPICall(fetchNSEAPI.buildPDVUrl(from, to, data.getTicker()));
//
//						queue.put(new WorkItem(data, response));

						fetchWithRetryAndQueue(data, queue);

					} catch (Exception e) {
						System.err.println("API error for " + data.getTicker());
					}
				});
			}

		} finally {
			shutdown(apiExecutor);
			shutdown(processExecutor);
			running.set(false);
			System.out.println("✅ PDV sync finished");
		}
	}

	// 🔥 Stage 2: CPU + DB work
	private void processAndPersist(WorkItem item) {

		DataFetchEntity data = item.data;
		JsonNode pdvResponse = item.response;

		TreeSet<PriceDeliveryVolumeEntity> pdvList = priceDeliveryVolumeService.parseStockData(pdvResponse,
				data.getTicker());

		if (!pdvList.isEmpty()) {
			Optional<TradeInfo> tradeInfoOpt = Optional.empty();

			pdvPersistenceService.persistAll(data.getTicker(), pdvList, tradeInfoOpt, pdvResponse);
		}
	}

	private String calculateToDate(String fromDate) {
		LocalDate from = LocalDate.parse(fromDate, FORMATTER);
		LocalDate today = LocalDate.now();
		return from.plusMonths(3).isBefore(today) ? from.plusMonths(3).format(FORMATTER) : today.format(FORMATTER);
	}

	private void shutdown(ExecutorService executor) {
		executor.shutdown();
		try {
			executor.awaitTermination(2, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// 🔥 Work item for pipeline
	private static class WorkItem {
		DataFetchEntity data;
		JsonNode response;

		WorkItem(DataFetchEntity data, JsonNode response) {
			this.data = data;
			this.response = response;
		}
	}

	private void fetchWithRetryAndQueue(DataFetchEntity data, BlockingQueue<WorkItem> queue) {

		String ticker = data.getTicker();

		LocalDate fromDate = data.getPdvtLastDate();
		LocalDate lastAttemptedToDate = fromDate;

		for (int attempt = 1; attempt <= 3; attempt++) {
			try {
				String from = fromDate.format(FORMATTER);
				String to = calculateToDate(from);

				lastAttemptedToDate = LocalDate.parse(to, FORMATTER);

				JsonNode response = fetchNSEAPI.NSEAPICall(fetchNSEAPI.buildPDVUrl(from, to, ticker));

				TreeSet<PriceDeliveryVolumeEntity> pdvList = priceDeliveryVolumeService.parseStockData(response,
						ticker);

				// ✅ SUCCESS CASE
				if (!pdvList.isEmpty()) {
					queue.put(new WorkItem(data, response));
					return;
				}

				System.out.println("Retry " + attempt + " empty for " + ticker);

				// 🔁 Move window forward
				fromDate = lastAttemptedToDate;

			} catch (Exception e) {
				System.err.println("Retry " + attempt + " failed for " + ticker);
			}
		}

		// ❌ AFTER ALL RETRIES FAILED
		System.out.println("No data after retries for " + ticker + " → updating DFHT");

		updateDfhtAfterFailure(data, lastAttemptedToDate);
	}

	private void updateDfhtAfterFailure(DataFetchEntity data, LocalDate lastAttemptedToDate) {
		try {
			dataFetchHistoryService.updateLastDateForPdvt(data.getTicker(), lastAttemptedToDate); // or update method
		} catch (Exception e) {
			System.err.println("Failed to update DFHT for " + data.getTicker());
		}
	}
}