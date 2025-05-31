package com.terminal_devilal.controllers.DataGathering;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.terminal_devilal.Utils.FetchNSEAPI;
import com.terminal_devilal.controllers.DataGathering.Model.DataFetchHistroy;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Service.PriceDeliveryVolumeService;
import com.terminal_devilal.controllers.DataGathering.Service.ProcessedDatesService;

@RestController
@RequestMapping("/pdv")
public class deliveryPriceVolumeController {

	// Setup date formatter to match API format like "25-05-2025"
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	// Define number of threads to run in parallel (max API calls at a time)
	private static final int THREAD_POOL_SIZE = 50;

	private static final int BATCH_LIMIT = 99;

	private static final int SLEEP_TIME_MS = 5000;

	private final ProcessedDatesService processedDatesService;

	private final PriceDeliveryVolumeService priceDeliveryVolumeService;

	private final FetchNSEAPI fetchNSEAPI;

	public deliveryPriceVolumeController(ProcessedDatesService processedDatesService,
			PriceDeliveryVolumeService priceDeliveryVolumeService, FetchNSEAPI fetchNSEAPI) {
		super();
		this.processedDatesService = processedDatesService;
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
		this.fetchNSEAPI = fetchNSEAPI;
	}

	@GetMapping("/revise-data")
	public void processPdvDataTillDate() {
		List<DataFetchHistroy> processedDates = processedDatesService.getProcessedDatesForTickers();
		int total = processedDates.size();

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		AtomicInteger counter = new AtomicInteger(0);

		for (int i = 0; i < total; i++) {
			DataFetchHistroy data = processedDates.get(i);

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
					String fromDate = data.getPdvtLastDate().format(FORMATTER);
					String toDate = LocalDate.now().format(FORMATTER);
					String url = this.fetchNSEAPI.buildPDVUrl(fromDate, toDate, data.getTicker());

					// Fetch Data
					JsonNode response = this.fetchNSEAPI.NSEAPICall(url);
					TreeSet<PriceDeliveryVolume> pdvList = this.priceDeliveryVolumeService.parseStockData(response,
							data.getTicker());

					// Save the Data
					priceDeliveryVolumeService.saveAllPdvList(new LinkedList<>(pdvList));

					// update last pdvt date
					processedDatesService.updateLastDateForPdvt(data.getTicker(), pdvList.last().getDate());

				} catch (IOException | InterruptedException e) {
					System.err.println("Error processing " + data.getTicker() + ": " + e.getMessage());
				}

				// Track and print progress
				int current = counter.incrementAndGet();
				double progress = (100.0 * current) / total;
				System.out.printf("Progress: %.2f%% (%d/%d completed)\n", progress, current, total);
			});
		}

		executor.shutdown();
	}

}