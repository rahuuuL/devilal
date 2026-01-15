package com.terminal_devilal.core_processes.sync_data.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.business_tools.trade_info.entities.TradeInfo;
import com.terminal_devilal.business_tools.trade_info.service.TradeInfoService;
import com.terminal_devilal.core_processes.dfht.entities.DataFetchEntity;
import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;
import com.terminal_devilal.indicators.pdv.entities.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.service.PriceDeliveryVolumeService;
import com.terminal_devilal.utils.nse.FetchNSEAPI;

@Service
public class DataSync {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	private static final int THREAD_POOL_SIZE = 30;
	private static final int MAX_API_CONCURRENCY = 8;
	private static final int MAX_RETRIES = 5;

	private final Semaphore apiLimiter = new Semaphore(MAX_API_CONCURRENCY);

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final DataFetchHistoryService dataFetchHistoryService;
	private final PriceDeliveryVolumeService priceDeliveryVolumeService;
	private final TradeInfoService tradeInfoService;
	private final FetchNSEAPI fetchNSEAPI;

	private final PdvPersistenceService pdvPersistenceService;

	public DataSync(DataFetchHistoryService dataFetchHistoryService,
			PriceDeliveryVolumeService priceDeliveryVolumeService, TradeInfoService tradeInfoService,
			FetchNSEAPI fetchNSEAPI, PdvPersistenceService pdvPersistenceService) {
		super();
		this.dataFetchHistoryService = dataFetchHistoryService;
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
		this.tradeInfoService = tradeInfoService;
		this.fetchNSEAPI = fetchNSEAPI;
		this.pdvPersistenceService = pdvPersistenceService;
	}

	public void processPdvDataTillDate() {

		if (!running.compareAndSet(false, true)) {
			System.out.println("⚠️ PDV sync already running. Skipping.");
			return;
		}

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

		try {
			List<DataFetchEntity> records = dataFetchHistoryService.getProcessedDatesForTickers().stream()
					.filter(d -> d.getPdvtLastDate().isBefore(LocalDate.now())).toList();

			int total = records.size();
			AtomicInteger completed = new AtomicInteger();

			System.out.println("Starting PDV sync for " + total + " tickers");

			for (DataFetchEntity data : records) {
				executor.submit(() -> processSingleTicker(data, completed, total));
			}

		} finally {
			executor.shutdown();
			try {
				executor.awaitTermination(2, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			running.set(false);
			System.out.println("✅ PDV sync finished");
		}
	}

	private void processSingleTicker(DataFetchEntity data, AtomicInteger counter, int total) {

		try {
			String from = data.getPdvtLastDate().format(FORMATTER);
			String to = calculateToDate(from);

			JsonNode pdvResponse = callApiWithLimit(fetchNSEAPI.buildPDVUrl(from, to, data.getTicker()));

			TreeSet<PriceDeliveryVolumeEntity> pdvList = priceDeliveryVolumeService.parseStockData(pdvResponse,
					data.getTicker());

			if (pdvList.isEmpty()) {
				pdvList = retryFetch(from, to, data.getTicker());
			}

			if (!pdvList.isEmpty()) {
				JsonNode tradeInfo = callApiWithLimit(fetchNSEAPI.buildTradeInfoUrl(data.getTicker()));

				Optional<TradeInfo> tradeInfoOpt = tradeInfoService.parseTradeInfo(tradeInfo, data.getTicker(),
						LocalDate.now());

				pdvPersistenceService.persistAll(data.getTicker(), pdvList, tradeInfoOpt, pdvResponse);
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			System.err.println("Error processing " + data.getTicker() + ": " + e.getMessage());
		} finally {
			int done = counter.incrementAndGet();
			System.out.printf("Progress: %.2f%% (%d/%d)%n", (100.0 * done) / total, done, total);
		}
	}

	private TreeSet<PriceDeliveryVolumeEntity> retryFetch(String from, String to, String ticker)
			throws InterruptedException {

		for (int i = 1; i <= MAX_RETRIES; i++) {
			Thread.sleep(2000L * i);

			try {
				JsonNode response = callApiWithLimit(fetchNSEAPI.buildPDVUrl(from, to, ticker));

				TreeSet<PriceDeliveryVolumeEntity> list = priceDeliveryVolumeService.parseStockData(response, ticker);

				if (!list.isEmpty())
					return list;

			} catch (IOException e) {
				System.err.println("Retry " + i + " failed for " + ticker);
			}
		}
		return new TreeSet<>();
	}

	private JsonNode callApiWithLimit(String url) throws IOException, InterruptedException {

		apiLimiter.acquire();
		try {
			return fetchNSEAPI.NSEAPICall(url);
		} finally {
			apiLimiter.release();
		}
	}

	private String calculateToDate(String fromDate) {
		LocalDate from = LocalDate.parse(fromDate, FORMATTER);
		LocalDate today = LocalDate.now();
		return from.plusMonths(3).isBefore(today) ? from.plusMonths(3).format(FORMATTER) : today.format(FORMATTER);
	}
}
