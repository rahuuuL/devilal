package com.terminal_devilal.controllers.Functional.VWAP;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.controllers.DataGathering.Model.VWAP;
import com.terminal_devilal.controllers.DataGathering.Service.VWAPService;
import com.terminal_devilal.controllers.Functional.VWAP.Model.VWAPDTO;

@RestController
@RequestMapping("/api/devilal/vwap")
public class VWAPController {
	private final VWAPService vwapService;

	public VWAPController(VWAPService vwapService) {
		this.vwapService = vwapService;
	}

	@GetMapping("/from")
	public Map<String, List<VWAP>> getVwapDataFromDate(
			@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
		return vwapService.getVwapDataFromDate(fromDate);
	}

	@GetMapping("/ticker/from")
	public List<VWAPDTO> getVWAPForTickerFromDate(@RequestParam("ticker") String ticker,
			@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
		return vwapService.getVWAPForTickerFromDate(ticker, fromDate).stream()
				.map(v -> new VWAPDTO(v.getDate(), v.getClosePrice(), v.getVwap(), v.getVwapProximity())).toList();
	}

}
