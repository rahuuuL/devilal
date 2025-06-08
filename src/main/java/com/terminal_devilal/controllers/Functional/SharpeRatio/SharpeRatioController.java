package com.terminal_devilal.controllers.Functional.SharpeRatio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.controllers.Functional.SharpeRatio.Model.SharpeRatioDTO;
import com.terminal_devilal.controllers.Functional.SharpeRatio.Service.SharpeRatioService;

@RestController
@RequestMapping("/api/devilal")
public class SharpeRatioController {

	private final SharpeRatioService sharpeRatioService;

	public SharpeRatioController(SharpeRatioService sharpeRatioService) {
		super();
		this.sharpeRatioService = sharpeRatioService;
	}

	@GetMapping("/sharpe/all")
	public Map<String, SharpeRatioDTO> getSharpeRatios(@RequestParam(name = "days", defaultValue = "30") int days,
			@RequestParam(name = "riskFreeRate", defaultValue = "0.06") double riskFreeRate) {
		LocalDate from = LocalDate.now().minusDays(days);
		return sharpeRatioService.computeSharpeRatios(from, riskFreeRate);
	}

	@GetMapping("/sharpe")
	public Map<String, SharpeRatioDTO> getSharpeRatios(@RequestParam(name = "days", defaultValue = "30") int days,
			@RequestParam(name = "riskFreeRate", defaultValue = "0.06") double riskFreeRate,
			@RequestParam(name = "tickers", required = true) List<String> tickers) {
		LocalDate from = LocalDate.now().minusDays(days);
		return sharpeRatioService.computeSharpeRatios(from, riskFreeRate, tickers);
	}

}
