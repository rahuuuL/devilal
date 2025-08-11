package com.terminal_devilal.utils.python_server_service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallAPIResponse;

@Service
public class PythonStatsServerAPIService {
	private static final Logger log = LoggerFactory.getLogger(PythonStatsServerAPIService.class);

	private final RestTemplate restTemplate;
	private final String manKandellUrl;

	public PythonStatsServerAPIService(RestTemplateBuilder builder,
			@Value("${batch-analysis-mann-kandell.api.url}") String manKandellUrl) {
		this.restTemplate = builder.build();
		this.manKandellUrl = manKandellUrl;
	}

	public List<MannKendallAPIResponse> analyzeBatch(Map<String, List<Double>> tickerPrices) {
		Map<String, Object> request = Map.of("data", tickerPrices);

		try {
			ResponseEntity<MannKendallAPIResponse[]> response = restTemplate.postForEntity(manKandellUrl, request,
					MannKendallAPIResponse[].class);

			MannKendallAPIResponse[] responses = response.getBody();
			if (responses == null) {
				return Collections.emptyList();
			}

			return Arrays.asList(responses);

		} catch (Exception e) {
			log.error("Failed to call Python batch Mann-Kendall API", e);
			return Collections.emptyList();
		}
	}
}
