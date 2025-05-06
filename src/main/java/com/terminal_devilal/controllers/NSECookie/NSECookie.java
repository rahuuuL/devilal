package com.terminal_devilal.controllers.NSECookie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.controllers.NSECookie.Exception.CookieReadException;
import com.terminal_devilal.controllers.NSECookie.Exception.CookieSaveException;
import com.terminal_devilal.controllers.NSECookie.Model.NSECookieRequest;

@RestController
@RequestMapping("/nse-cookie")
public class NSECookie {
	
	private static final Path FILE_NAME = Paths.get("NSE_COOKIE.properties");

    @PostMapping("/save")
    public ResponseEntity<String> saveCookie(@RequestBody NSECookieRequest request) throws IOException {
        try {
            Files.writeString(FILE_NAME, request.getCookieValue());
            return ResponseEntity.ok("Cookie saved to : " + FILE_NAME);
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

}
