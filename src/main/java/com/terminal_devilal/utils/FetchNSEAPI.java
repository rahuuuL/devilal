package com.terminal_devilal.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FetchNSEAPI {
	
	private static final Path FILE_NAME = Paths.get("NSE_COOKIE.properties");
	
	public static JsonNode NSEAPICall(String url) throws IOException, InterruptedException {
		
		// NSE API call preparation
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
        	    .uri(URI.create(url))
        	    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/122.0.0.0 Safari/537.36")
        	    .header("Accept", "application/json, text/javascript, */*; q=0.01")
        	    .header("Accept-Language", "en-US,en;q=0.9")
        	    .header("Referer", "https://www.nseindia.com/")
        	    .header("Origin", "https://www.nseindia.com")
        	    .header("X-Requested-With", "XMLHttpRequest")
        	    .header("Cookie", Files.readString(FILE_NAME))
        	    .build();
        
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

}
