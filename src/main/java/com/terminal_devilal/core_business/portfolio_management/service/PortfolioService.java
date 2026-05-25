package com.terminal_devilal.core_business.portfolio_management.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.core_business.portfolio_management.dto.InvestmentEntryDTO;
import com.terminal_devilal.core_business.portfolio_management.dto.PortfolioDTO;
import com.terminal_devilal.core_business.portfolio_management.entity.InvestmentEntry;
import com.terminal_devilal.core_business.portfolio_management.entity.Portfolio;
import com.terminal_devilal.core_business.portfolio_management.repository.InvestmentEntryDAO;
import com.terminal_devilal.core_business.portfolio_management.repository.PortfolioDAO;

@Service
@Transactional
public class PortfolioService {

	private final PortfolioDAO portfolioDAO;
	private final InvestmentEntryDAO entryDAO;

	public PortfolioService(PortfolioDAO portfolioDAO, InvestmentEntryDAO entryDAO) {
		this.portfolioDAO = portfolioDAO;
		this.entryDAO = entryDAO;
	}

	// ── 1. Get all active portfolios ────────────────────────────────────────

	@Transactional(readOnly = true)
	public List<PortfolioDTO.PortfolioResponse> getAllPortfolios() {
		return portfolioDAO.findAllActiveWithInvestments().stream().map(this::toResponse).toList();
	}

	// ── 2. Create portfolio ──────────────────────────────────────────────────

	public PortfolioDTO.PortfolioResponse createPortfolio(PortfolioDTO.CreatePortfolioRequest request) {
		if (portfolioDAO.existsByName(request.name())) {
			throw new IllegalArgumentException("Portfolio with name '" + request.name() + "' already exists.");
		}

		Portfolio portfolio = new Portfolio(request.name());
		return toResponse(portfolioDAO.save(portfolio));
	}

	// ── 3. Add investment entry ──────────────────────────────────────────────

	public PortfolioDTO.PortfolioResponse addInvestmentEntry(String portfolioName,
			InvestmentEntryDTO.AddEntryRequest request) {

		Portfolio portfolio = findActivePortfolio(portfolioName);

		if (entryDAO.existsById_TickerAndId_PortfolioName(request.ticker(), portfolioName)) {
			throw new IllegalArgumentException("Ticker '" + request.ticker() + "' already exists in portfolio '"
					+ portfolioName + "'. " + "Use the update endpoint to change quantity or price.");
		}

		LocalDate addedDate = request.addedDate() != null ? request.addedDate() : LocalDate.now();

		InvestmentEntry entry = new InvestmentEntry(request.ticker(), portfolio, request.price(), request.quantity(),
				addedDate);

		portfolio.getInvestments().add(entry);
		portfolio.setLastUpdatedDate(LocalDate.now());

		return toResponse(portfolioDAO.save(portfolio));
	}

	// ── 4. Remove investment entry (hard delete) ─────────────────────────────

	public PortfolioDTO.PortfolioResponse removeInvestmentEntry(String portfolioName, String ticker) {
		Portfolio portfolio = findActivePortfolio(portfolioName);

		InvestmentEntry entry = entryDAO.findById_TickerAndId_PortfolioName(ticker, portfolioName)
				.orElseThrow(() -> new IllegalArgumentException(
						"Ticker '" + ticker + "' not found in portfolio '" + portfolioName + "'."));

		// Removing from the list triggers orphanRemoval — JPA will DELETE the row.
		portfolio.getInvestments().remove(entry);
		portfolio.setLastUpdatedDate(LocalDate.now());

		return toResponse(portfolioDAO.save(portfolio));
	}

	// ── 5. Update investment entry ───────────────────────────────────────────

	public PortfolioDTO.PortfolioResponse updateInvestmentEntry(String portfolioName, String ticker,
			InvestmentEntryDTO.UpdateEntryRequest request) {

		Portfolio portfolio = findActivePortfolio(portfolioName);

		InvestmentEntry entry = entryDAO.findById_TickerAndId_PortfolioName(ticker, portfolioName)
				.orElseThrow(() -> new IllegalArgumentException(
						"Ticker '" + ticker + "' not found in portfolio '" + portfolioName + "'."));

		entry.setPrice(request.price());
		entry.setQuantity(request.quantity());

		// Touch the portfolio's lastUpdatedDate on every entry change.
		portfolio.setLastUpdatedDate(LocalDate.now());

		entryDAO.save(entry);
		return toResponse(portfolioDAO.save(portfolio));
	}

	// ── 6. Soft-delete portfolio (sets active = false) ───────────────────────

	public PortfolioDTO.PortfolioResponse deactivatePortfolio(String portfolioName) {
		Portfolio portfolio = findActivePortfolio(portfolioName);
		portfolio.setActive(false);
		portfolio.setLastUpdatedDate(LocalDate.now()); // record when it was deactivated
		return toResponse(portfolioDAO.save(portfolio));
	}

	// ── Private helpers ──────────────────────────────────────────────────────

	private Portfolio findActivePortfolio(String name) {
		return portfolioDAO.findByNameAndActiveTrue(name)
				.orElseThrow(() -> new IllegalArgumentException("Active portfolio '" + name + "' not found."));
	}

	private PortfolioDTO.PortfolioResponse toResponse(Portfolio portfolio) {

		List<InvestmentEntryDTO.EntryResponse> entries = portfolio.getInvestments().stream()
				.map(e -> new InvestmentEntryDTO.EntryResponse(e.getTicker(), e.getPrice(), e.getQuantity(),
						e.getAddedDate()))
				.toList();

		return new PortfolioDTO.PortfolioResponse(portfolio.getName(), portfolio.isActive(),
				portfolio.getLastUpdatedDate(), entries);
	}
}