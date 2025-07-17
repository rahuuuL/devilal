package com.terminal_devilal.controllers.Functional.BountyHunting;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.controllers.Functional.BountyHunting.Model.BountyHuntingDTO;
import com.terminal_devilal.controllers.Functional.BountyHunting.Service.BountyHuntingService;

@RestController
@RequestMapping("/api/devilal")
public class BountyHuntingController {

	private BountyHuntingService bountyHuntingService;

	public BountyHuntingController(BountyHuntingService bountyHuntingService) {
		super();
		this.bountyHuntingService = bountyHuntingService;
	}

	@GetMapping("/hunt")
	public ResponseEntity<List<BountyHuntingDTO>> getNextHunt(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(defaultValue = "6.0") double riskFreeRate) {

		// If fromDate not provided, default to 30 days ago (or any logic you prefer)
		if (fromDate == null) {
			fromDate = LocalDate.now().minusDays(30);
		}

		List<BountyHuntingDTO> result = bountyHuntingService.getNextHunt(fromDate, riskFreeRate);
		return ResponseEntity.ok(result);
	}

}
