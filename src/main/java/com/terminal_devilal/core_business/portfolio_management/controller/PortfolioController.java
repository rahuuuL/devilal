package com.terminal_devilal.core_business.portfolio_management.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.core_business.portfolio_management.dto.InvestmentEntryDTO;
import com.terminal_devilal.core_business.portfolio_management.dto.PortfolioDTO;
import com.terminal_devilal.core_business.portfolio_management.entity.Portfolio;
import com.terminal_devilal.core_business.portfolio_management.service.PortfolioService;

@RestController
@RequestMapping("/api/devilal/portfolio")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	// 1. Create portfolio with investments
	@PostMapping
	public Portfolio createPortfolio(@RequestBody PortfolioDTO dto) {
		return portfolioService.createPortfolio(dto);
	}

	// 2. Update portfolio name/description
	@PutMapping("/{id}")
	public Portfolio updatePortfolio(@PathVariable Long id, @RequestBody PortfolioDTO dto) {
		return portfolioService.updatePortfolio(id, dto);
	}

	// 3. Update investment entries only
	@PutMapping("/{id}/entries")
	public Portfolio updateEntries(@PathVariable Long id, @RequestBody List<InvestmentEntryDTO> entries) {
		return portfolioService.updateInvestmentEntries(id, entries);
	}

	// 4. Delete one investment entry from a portfolio
	@DeleteMapping("/{id}/entries/{entryId}")
	public void deleteInvestmentEntry(@PathVariable Long id, @PathVariable Long entryId) {
		portfolioService.deleteInvestmentEntry(id, entryId);
	}

	// 5. Delete the entire portfolio and its investments
	@DeleteMapping("/{id}")
	public void deletePortfolio(@PathVariable Long id) {
		portfolioService.deletePortfolio(id);
	}
}
