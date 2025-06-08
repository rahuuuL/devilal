package com.terminal_devilal.controllers.Functional.Beta;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.controllers.Functional.Beta.Model.BetaDTO;
import com.terminal_devilal.controllers.Functional.Beta.Service.BetaCalculator;

@RestController
@RequestMapping("/api/devilal")
public class BetaController {

	private final BetaCalculator betaCalculator;

	public BetaController(BetaCalculator betaCalculator) {
		super();
		this.betaCalculator = betaCalculator;
	}

	@GetMapping("/beta")
	public BetaDTO getBeta(@RequestParam(name = "tickers", required = true) List<String> tickers,
			@RequestParam(name = "from") LocalDate from) {

		BetaDTO response = new BetaDTO(0, 0);
		response.setBetaWRTAtoB(betaCalculator.calculateBeta(tickers.get(0), tickers.get(1), from));
		response.setBetaWRTBtoA(betaCalculator.calculateBeta(tickers.get(1), tickers.get(0), from));

		return response;

	}

}
