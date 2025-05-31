package com.terminal_devilal.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FetchNSEAPI {

	private final Path FILE_NAME = Paths.get("NSE_COOKIE.properties");

	private final String PDV_BASE_URL = "https://www.nseindia.com/api/historicalOR/generateSecurityWiseHistoricalData?from=%s&to=%s&symbol=%s&type=priceVolumeDeliverable&series=EQ";

	public JsonNode NSEAPICall(String url) throws IOException, InterruptedException {

		// NSE API call preparation
		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
				.header("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/122.0.0.0 Safari/537.36")
				.header("Accept", "application/json, text/javascript, */*; q=0.01")
				.header("Accept-Language", "en-US,en;q=0.9").header("Referer", "https://www.nseindia.com/")
				.header("Origin", "https://www.nseindia.com").header("X-Requested-With", "XMLHttpRequest")
				.header("Cookie", Files.readString(FILE_NAME)).build();

		// Call NSE API
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new IOException("Failed: " + response.statusCode() + " - " + response.body());
		}

		// Convert to JSON
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(response.body());

//		if (!isValidPDVData(json)) {
//			throw new InvalidOrEmptyNSEData("Failed: " + "Invalid PDV response " + " - " + json.toPrettyString());
//		}

		return json;
	}

	/**
	 * Builds the NSE URL by replacing placeholder.
	 * 
	 * @param fromDate the start date (e.g., "01-05-2025")
	 * @param toDate   the end date (e.g., "05-05-2025")
	 * @param symbol   the stock symbol (e.g., "INFY")
	 * @return a complete URL with values inserted
	 */
	public String buildPDVUrl(String fromDate, String toDate, String symbol) {
		String encodedSymbol = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
		return String.format(PDV_BASE_URL, fromDate, toDate, encodedSymbol);
	}

//	private boolean isValidPDVData(JsonNode node) {
//		JsonNode data = node.path("data");
//
//		// Ensure "data" is an object, not null/array/missing
//		if (data.isMissingNode() || !data.isObject()) {
//			System.err.println("Invalid format: 'data' is missing or not an object.");
//			return false;
//		}
//
//		// Check for required fields
//		String[] requiredFields = { "CH_SYMBOL", "CH_SERIES", "mTIMESTAMP", "CH_PREVIOUS_CLS_PRICE", "CH_OPENING_PRICE",
//				"CH_TRADE_HIGH_PRICE", "CH_TRADE_LOW_PRICE", "CH_LAST_TRADED_PRICE", "CH_CLOSING_PRICE", "VWAP",
//				"CH_TOT_TRADED_QTY", "CH_TOT_TRADED_VAL", "CH_TOTAL_TRADES", "CH_TIMESTAMP", "COP_DELIV_QTY",
//				"COP_DELIV_PERC" };
//
//		for (String field : requiredFields) {
//			if (!data.has(field) || data.get(field).isNull()) {
//				System.err.println("Missing or null field: " + field);
//				return false;
//			}
//		}
//
//		return true; // Everything looks fine
//	}

}
