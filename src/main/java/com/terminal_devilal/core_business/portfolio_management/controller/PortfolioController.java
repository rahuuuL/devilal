package com.terminal_devilal.core_business.portfolio_management.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.core_business.portfolio_management.dto.InvestmentEntryDTO;
import com.terminal_devilal.core_business.portfolio_management.dto.PortfolioDTO;
import com.terminal_devilal.core_business.portfolio_management.service.PortfolioService;

@RestController
@RequestMapping("/api/devilal/portfolio")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	// ── GET /api/devilal/portfolio ───────────────────────────────────────────
	// Returns all active portfolios with their investment entries.
	@GetMapping
	public List<PortfolioDTO.PortfolioResponse> getAllPortfolios() {
		return portfolioService.getAllPortfolios();
	}

	// ── POST /api/devilal/portfolio ──────────────────────────────────────────
	// Create a new portfolio. Name must be unique.
	// Body: { "name": "Primary", "description": "My main portfolio" }
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PortfolioDTO.PortfolioResponse createPortfolio(@RequestBody PortfolioDTO.CreatePortfolioRequest request) {
		return portfolioService.createPortfolio(request);
	}

	// ── POST /api/devilal/portfolio/{name}/entries ───────────────────────────
	// Add a ticker (investment entry) to an existing active portfolio.
	// Body: { "ticker": "RELIANCE", "price": 2500.0, "quantity": 10,
	// "addedDate": "2024-01-15", "riskFreeRate": 6.5 }
	// addedDate is optional — defaults to today.
	@PostMapping("/{name}/entries")
	@ResponseStatus(HttpStatus.CREATED)
	public PortfolioDTO.PortfolioResponse addInvestmentEntry(@PathVariable String name,
			@RequestBody InvestmentEntryDTO.AddEntryRequest request) {
		return portfolioService.addInvestmentEntry(name, request);
	}

	// ── DELETE /api/devilal/portfolio/{name}/entries/{ticker} ────────────────
	// Hard-delete a single investment entry from a portfolio.
	// The row is permanently removed from investment_entry table.
	@DeleteMapping("/{name}/entries/{ticker}")
	public PortfolioDTO.PortfolioResponse removeInvestmentEntry(@PathVariable String name,
			@PathVariable String ticker) {
		return portfolioService.removeInvestmentEntry(name, ticker);
	}

	// ── PATCH /api/devilal/portfolio/{name}/entries/{ticker} ─────────────────
	// Update price, quantity, or riskFreeRate on an existing entry.
	// ticker and addedDate are immutable.
	// Body: { "price": 2600.0, "quantity": 15, "riskFreeRate": 6.8 }
	@PatchMapping("/{name}/entries/{ticker}")
	public PortfolioDTO.PortfolioResponse updateInvestmentEntry(@PathVariable String name, @PathVariable String ticker,
			@RequestBody InvestmentEntryDTO.UpdateEntryRequest request) {
		return portfolioService.updateInvestmentEntry(name, ticker, request);
	}

	// ── DELETE /api/devilal/portfolio/{name} ─────────────────────────────────
	// Soft-delete: sets active = false and records lastUpdatedDate.
	// The portfolio and its entries remain in the database.
	@DeleteMapping("/{name}")
	public PortfolioDTO.PortfolioResponse deactivatePortfolio(@PathVariable String name) {
		return portfolioService.deactivatePortfolio(name);
	}
}