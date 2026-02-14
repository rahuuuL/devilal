package com.terminal_devilal.core_processes.ticker_details.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.terminal_devilal.core_processes.ticker_details.model.TickerDetailsResponse;
import com.terminal_devilal.core_processes.ticker_details.repository.TickerDetailsRepository;

@Service
public class TickerDetailsService {

	private Map<String, TickerDetailsResponse> tickerDetails;

	public TickerDetailsService(TickerDetailsRepository repository) {

		List<TickerDetailsResponse> list = repository.fetchAllTickerDetailsFast();

		Map<String, TickerDetailsResponse> tempMap = new HashMap<>(list.size());

		for (TickerDetailsResponse r : list) {
			tempMap.put(r.getTicker(), r);
		}

		this.tickerDetails = Collections.unmodifiableMap(tempMap);
	}

	public Map<String, TickerDetailsResponse> getTickerDetails() {
		return tickerDetails;
	}
}
