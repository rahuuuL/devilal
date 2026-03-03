package com.terminal_devilal.indicators.price.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.indicators.price.model.RollingSlopeResult;
import com.terminal_devilal.indicators.price.service.RollingSlopeCalculator;

@RestController
@RequestMapping("/api/devilal/price-analysis")
public class RollingSlopeController {
	private final RollingSlopeCalculator rollingSlopeCalculator;

	public RollingSlopeController(RollingSlopeCalculator rollingSlopeCalculator) {
		super();
		this.rollingSlopeCalculator = rollingSlopeCalculator;
	}

	@GetMapping("/rolling-slope")
	public ResponseEntity<List<RollingSlopeResult>> getRollingSlope(@RequestParam LocalDate fromDate,
			@RequestParam LocalDate toDate, @RequestParam int windowSize) {

		if (fromDate.isAfter(toDate)) {
			return ResponseEntity.badRequest().build();
		}

		List<RollingSlopeResult> result = rollingSlopeCalculator.getRollingSlope(fromDate, toDate, windowSize);

		return ResponseEntity.ok(result);
	}
}
