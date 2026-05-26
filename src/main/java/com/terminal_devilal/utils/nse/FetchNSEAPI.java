package com.terminal_devilal.utils.nse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private final String TRADE_INFO = "https://www.nseindia.com/api/NextApi/apiClient/GetQuoteApi?functionName=getSymbolData&marketType=N&series=EQ&symbol=%s";

	private final String TICKER_INFO = "https://www.nseindia.com/api/quote-equity?symbol=%s";

	private final RestTemplate restTemplate = new RestTemplate();

	private static final Logger log = LoggerFactory.getLogger(FetchNSEAPI.class);
	
	public FetchNSEAPI(StaticCache cache) {
		this.cache = cache;
	}


	public JsonNode NSEAPICall(String url) throws IOException, InterruptedException {

	    log.info("NSE API call initiated | url={}", url);

	    HttpClient client = HttpClient.newHttpClient();

	    HttpRequest request = HttpRequest.newBuilder()
	            .uri(URI.create(url))
	            .header("User-Agent",
	                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/122.0.0.0 Safari/537.36")
	            .header("Accept", "application/json, text/javascript, */*; q=0.01")
	            .header("Accept-Language", "en-US,en;q=0.9")
	            .header("Referer", "https://www.nseindia.com/")
	            .header("Origin", "https://www.nseindia.com")
	            .header("X-Requested-With", "XMLHttpRequest")
	            .header("Cookie", cache.get(StaticCache.COOKIE))
	            .build();

	    HttpResponse<String> response;
	    try {
	        response = client.send(request, HttpResponse.BodyHandlers.ofString());
	    } catch (IOException e) {
	        log.error("NSE API call failed at network level | url={} | error={}", url, e.getMessage(), e);
	        throw e;
	    } catch (InterruptedException e) {
	        log.error("NSE API call interrupted | url={}", url, e);
	        Thread.currentThread().interrupt(); // restore interrupt flag
	        throw e;
	    }

	    int statusCode = response.statusCode();
	    log.debug("NSE API response received | url={} | status={} | bodyLength={}",
	            url, statusCode, response.body() == null ? 0 : response.body().length());

	    if (statusCode != 200) {
	        // Trim body in log to avoid flooding logs with huge HTML error pages
	        String bodySnippet = response.body() != null
	                ? response.body().substring(0, Math.min(500, response.body().length()))
	                : "<empty>";
	        log.error("NSE API returned non-200 | url={} | status={} | bodySnippet={}",
	                url, statusCode, bodySnippet);
	        throw new IOException("NSE API failed | status=" + statusCode + " | url=" + url + " | body=" + bodySnippet);
	    }

	    log.info("NSE API call successful | url={} | status={}", url, statusCode);

	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        JsonNode json = mapper.readTree(response.body());
	        return json;
	    } catch (IOException e) {
	        log.error("Failed to parse NSE API response as JSON | url={} | body={}",
	                url, response.body(), e);
	        throw e;
	    }
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
