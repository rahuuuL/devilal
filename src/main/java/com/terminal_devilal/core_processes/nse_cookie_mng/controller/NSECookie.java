package com.terminal_devilal.core_processes.nse_cookie_mng.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.core_processes.nse_cookie_mng.entity.NSECookieRequest;
import com.terminal_devilal.core_processes.nse_cookie_mng.entity.SaveCookieResponse;
import com.terminal_devilal.core_processes.nse_cookie_mng.exception.CookieReadException;
import com.terminal_devilal.core_processes.nse_cookie_mng.exception.CookieSaveException;
import com.terminal_devilal.utils.nse.FetchNSEAPI;

@RestController
@RequestMapping("/nse-cookie")
public class NSECookie {

	private final Path FILE_NAME = Paths.get("NSE_COOKIE.properties");

	private final FetchNSEAPI fetchNSEAPI;

	public NSECookie(FetchNSEAPI fetchNSEAPI) {
		super();
		this.fetchNSEAPI = fetchNSEAPI;
	}

	@PostMapping("/save")
	public ResponseEntity<SaveCookieResponse> saveCookie(@RequestBody NSECookieRequest request) throws IOException {
		try {
			Files.writeString(FILE_NAME, request.getCookieValue());
			SaveCookieResponse res = new SaveCookieResponse("Cookie saved");
			return ResponseEntity.ok(res);
		} catch (IOException e) {
			throw new CookieSaveException("Failed to save cookie", e);
		}
	}

	@GetMapping("/read")
	public ResponseEntity<String> readCookie() {
		try {
			return ResponseEntity.ok(Files.readString(FILE_NAME));
		} catch (IOException e) {
			throw new CookieReadException("Failed to read cookie file", e);
		}

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
