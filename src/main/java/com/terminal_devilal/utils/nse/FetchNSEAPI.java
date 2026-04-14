package com.terminal_devilal.utils.nse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terminal_devilal.core_processes.static_cache.StaticCache;
import com.terminal_devilal.core_processes.sync_data.dto.NseQuoteResponse;

@Service
public class FetchNSEAPI {

	private final StaticCache cache;

	private final String PDV_BASE_URL = "https://www.nseindia.com/api/historicalOR/generateSecurityWiseHistoricalData?from=%s&to=%s&symbol=%s&type=priceVolumeDeliverable&series=EQ";

	private final String TRADE_INFO = "https://www.nseindia.com/api/quote-equity?symbol=%s&section=trade_info";

	private final String TICKER_INFO = "https://www.nseindia.com/api/quote-equity?symbol=%s";

	private final RestTemplate restTemplate = new RestTemplate();

	public FetchNSEAPI(StaticCache cache) {
		this.cache = cache;
	}

	public JsonNode NSEAPICall(String url) throws IOException, InterruptedException {

		// NSE API call preparation
		HttpClient client = HttpClient.newHttpClient();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
				.header("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/122.0.0.0 Safari/537.36")
				.header("Accept", "application/json, text/javascript, */*; q=0.01")
				.header("Accept-Language", "en-US,en;q=0.9").header("Referer", "https://www.nseindia.com/")
				.header("Origin", "https://www.nseindia.com").header("X-Requested-With", "XMLHttpRequest")
				.header("Cookie", cache.get(StaticCache.COOKIE)).build();

		// Call NSE API
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new IOException("Failed: " + response.statusCode() + " - " + response.body());
		}

		// Convert to JSON
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(response.body());

		return json;
	}

	public NseQuoteResponse fetchQuote(String symbol) {

		HttpHeaders headers = new HttpHeaders();

		headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
		headers.set(HttpHeaders.ACCEPT, "application/json");
		headers.set(HttpHeaders.REFERER, "https://www.nseindia.com");

		HttpEntity<Void> entity = new HttpEntity<>(headers);

		ResponseEntity<NseQuoteResponse> response = restTemplate.exchange(buildTickerInfoUrl(symbol), HttpMethod.GET,
				entity, NseQuoteResponse.class, symbol);

		return response.getBody();

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

	/**
	 * Builds the NSE Trade Info URL by replacing the symbol placeholder.
	 *
	 * @param symbol the stock symbol (e.g., "INFY")
	 * @return a complete URL with the symbol inserted and properly encoded
	 */
	public String buildTradeInfoUrl(String symbol) {
		String encodedSymbol = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
		return String.format(TRADE_INFO, encodedSymbol);
	}

	public String buildTickerInfoUrl(String ticker) {
		String encodedSymbol = URLEncoder.encode(ticker, StandardCharsets.UTF_8);
		return String.format(TICKER_INFO, encodedSymbol);
	}

}
