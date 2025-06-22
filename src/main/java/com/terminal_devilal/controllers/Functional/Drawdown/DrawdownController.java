package com.terminal_devilal.controllers.Functional.Drawdown;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.controllers.Functional.Drawdown.Model.DrawdownDTO;
import com.terminal_devilal.controllers.Functional.Drawdown.Service.DrawdownService;

@RestController
@RequestMapping("/api/devilal/drawdown")
public class DrawdownController {

	private final DrawdownService drawdownService;

	public DrawdownController(DrawdownService drawdownService) {
		this.drawdownService = drawdownService;
	}

	// 1. Drawdown for a specific ticker from a date
	@GetMapping("/drawdown")
	public List<DrawdownDTO> getDrawdownData(@RequestParam String ticker,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
		return drawdownService.calculateDrawdowns(ticker, fromDate);
	}

	// 2. Drawdown for all stocks from a date
	@GetMapping("/drawdown/all")
	public Map<String, List<DrawdownDTO>> getDrawdownsForAll(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
		return drawdownService.calculateDrawdownsForAll(fromDate);
	}
}
