package com.terminal_devilal.controllers.DataGathering;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

	private final String BASE_URL = "https://www.nseindia.com/api/historicalOR/generateSecurityWiseHistoricalData?from=%s&to=%s&symbol=%s&type=priceVolumeDeliverable&series=EQ";

	// Setup date formatter to match API format like "25-05-2025"
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	// Define number of threads to run in parallel (max API calls at a time)
	private static final int THREAD_POOL_SIZE = 50;

	private static final int BATCH_LIMIT = 99;

	private static final int SLEEP_TIME_MS = 5000;

	private ProcessedDatesService processedDatesService;

	private PriceDeliveryVolumeService priceDeliveryVolumeService;

	public deliveryPriceVolumeController(ProcessedDatesService processedDatesService,
			PriceDeliveryVolumeService priceDeliveryVolumeService) {
		this.processedDatesService = processedDatesService;
		this.priceDeliveryVolumeService = priceDeliveryVolumeService;
	}

	/**
	 * Builds the NSE URL by replacing placeholder.
	 * 
	 * @param fromDate the start date (e.g., "01-05-2025")
	 * @param toDate   the end date (e.g., "05-05-2025")
	 * @param symbol   the stock symbol (e.g., "INFY")
	 * @return a complete URL with values inserted
	 */
	private String buildUrl(String fromDate, String toDate, String symbol) {
		String encodedSymbol = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
		return String.format(BASE_URL, fromDate, toDate, encodedSymbol);
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
					String url = buildUrl(fromDate, toDate, data.getTicker());

					// Fetch Data
					JsonNode response = FetchNSEAPI.NSEAPICall(url);
					TreeSet<PriceDeliveryVolume> pdvList = PriceDeliveryVolume.parseStockData(response, data.getTicker());

					// Save the Data
					priceDeliveryVolumeService.saveAllPdvList(new LinkedList<>(pdvList));
					
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

	public static boolean isValidNSEData(JsonNode node) {
		JsonNode data = node.path("data");

		// Ensure "data" is an object, not null/array/missing
		if (data.isMissingNode() || !data.isObject()) {
			System.err.println("Invalid format: 'data' is missing or not an object.");
			return false;
		}

		// Check for required fields
		String[] requiredFields = { "CH_SYMBOL", "CH_SERIES", "mTIMESTAMP", "CH_PREVIOUS_CLS_PRICE", "CH_OPENING_PRICE",
				"CH_TRADE_HIGH_PRICE", "CH_TRADE_LOW_PRICE", "CH_LAST_TRADED_PRICE", "CH_CLOSING_PRICE", "VWAP",
				"CH_TOT_TRADED_QTY", "CH_TOT_TRADED_VAL", "CH_TOTAL_TRADES", "CH_TIMESTAMP", "COP_DELIV_QTY",
				"COP_DELIV_PERC" };

		for (String field : requiredFields) {
			if (!data.has(field) || data.get(field).isNull()) {
				System.err.println("Missing or null field: " + field);
				return false;
			}
		}

		return true; // Everything looks fine
	}

}
// produce data to kakfta
// consumer to comsume the data
