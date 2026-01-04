package com.terminal_devilal.core_processes.sync_data.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.terminal_devilal.core_processes.dfht.entities.DataFetchEntity;
import com.terminal_devilal.core_processes.dfht.service.DataFetchHistoryService;
import com.terminal_devilal.core_processes.sync_data.dto.NseQuoteResponse;
import com.terminal_devilal.core_processes.sync_data.entity.TickerIndustryInfo;
import com.terminal_devilal.core_processes.sync_data.repository.TickerIndustryInfoRepository;
import com.terminal_devilal.utils.nse.FetchNSEAPI;

import jakarta.transaction.Transactional;

@Service
public class TickerIndustryInfoUpdate {

	private static final int THREAD_POOL_SIZE = 10;
	private static final int MAX_API_CONCURRENCY = 4;

	private final FetchNSEAPI fetchNSEAPI;
	private final TickerIndustryInfoRepository repo;
	private final DataFetchHistoryService dataFetchHistoryService;

	private final Semaphore apiLimiter = new Semaphore(MAX_API_CONCURRENCY);
	private final AtomicBoolean running = new AtomicBoolean(false);

	public TickerIndustryInfoUpdate(FetchNSEAPI fetchNSEAPI, TickerIndustryInfoRepository repo,
			DataFetchHistoryService dataFetchHistoryService) {

		this.fetchNSEAPI = fetchNSEAPI;
		this.repo = repo;
		this.dataFetchHistoryService = dataFetchHistoryService;
	}

	@Transactional
	public void updateCompanyIndustryData() {

		if (!running.compareAndSet(false, true)) {
			System.out.println("⚠️ Industry sync already running");
			return;
		}

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

		try {
			List<DataFetchEntity> records = dataFetchHistoryService.getProcessedDatesForTickers();

			System.out.println("▶ Industry sync started for " + records.size() + " tickers");

			for (DataFetchEntity record : records) {
				executor.submit(() -> processSingleTicker(record));
			}

		} finally {
			executor.shutdown();
			try {
				executor.awaitTermination(1, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			running.set(false);
			System.out.println("✅ Industry sync completed");
		}
	}

	private void processSingleTicker(DataFetchEntity record) {

		String symbol = record.getTicker();

		try {
			apiLimiter.acquire();

			NseQuoteResponse response = fetchNSEAPI.fetchQuote(symbol);

			if (response == null || response.getInfo() == null || response.getIndustryInfo() == null) {
				System.out.println("⚠️ No industry data for " + symbol);
				return;
			}

			TickerIndustryInfo entity = new TickerIndustryInfo();

			entity.setTicker(response.getInfo().getSymbol());
			entity.setCompanyName(response.getInfo().getCompanyName());
			entity.setIsin(response.getInfo().getIsin());

			entity.setMacro(response.getIndustryInfo().getMacro());
			entity.setSector(response.getIndustryInfo().getSector());
			entity.setIndustry(response.getIndustryInfo().getIndustry());
			entity.setBasicIndustry(response.getIndustryInfo().getBasicIndustry());

			repo.save(entity);

			System.out.println("✔ Saved industry data for " + symbol);

		} catch (Exception ex) {
			System.err.println("❌ Failed for " + symbol + " : " + ex.getMessage());
		} finally {
			apiLimiter.release();
		}
	}
}
