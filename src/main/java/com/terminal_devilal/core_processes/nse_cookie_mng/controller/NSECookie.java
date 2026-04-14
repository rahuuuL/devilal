package com.terminal_devilal.core_processes.nse_cookie_mng.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.core_processes.nse_cookie_mng.entity.NSECookieRequest;
import com.terminal_devilal.core_processes.nse_cookie_mng.entity.SaveCookieResponse;
import com.terminal_devilal.core_processes.static_cache.StaticCache;
import com.terminal_devilal.utils.nse.FetchNSEAPI;

@RestController
@RequestMapping("/nse-cookie")
public class NSECookie {

	private final FetchNSEAPI fetchNSEAPI;

	private final StaticCache cache;

	public NSECookie(FetchNSEAPI fetchNSEAPI, StaticCache cache) {
		super();
		this.fetchNSEAPI = fetchNSEAPI;
		this.cache = cache;
	}

	@PostMapping("/save")
	public ResponseEntity<SaveCookieResponse> saveCookie(@RequestBody NSECookieRequest request) {
		this.cache.set(StaticCache.COOKIE, request.getCookieValue());
		SaveCookieResponse res = new SaveCookieResponse("Cookie saved");
		return ResponseEntity.ok(res);
	}

	@GetMapping("/read")
	public ResponseEntity<String> readCookie() {
		return ResponseEntity.ok(this.cache.get(StaticCache.COOKIE));

	}

	@GetMapping("/is-cookie-valid")
	public ResponseEntity<SaveCookieResponse> isCookieValid() throws IOException, InterruptedException {

		try {
			String ticker = "HAL";

			String url = String.format("https://www.nseindia.com/api/quote-equity?symbol=%s&section=trade_info",
					ticker);

			this.fetchNSEAPI.NSEAPICall(url);
			SaveCookieResponse res = new SaveCookieResponse("Valid", true);

			return ResponseEntity.ok(res);
		} catch (Exception e) {
			SaveCookieResponse res = new SaveCookieResponse("Invalid", false);
			return ResponseEntity.ok(res);
		}
	}

}
