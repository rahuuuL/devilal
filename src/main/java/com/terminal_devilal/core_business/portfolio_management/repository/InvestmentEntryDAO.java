package com.terminal_devilal.core_business.portfolio_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.terminal_devilal.core_business.portfolio_management.entity.InvestmentEntry;
import com.terminal_devilal.core_business.portfolio_management.entity.InvestmentEntryId;

/**
 * PK is InvestmentEntryId (composite: ticker + portfolioName). Was incorrectly
 * Long in the original.
 */
public interface InvestmentEntryDAO extends JpaRepository<InvestmentEntry, InvestmentEntryId> {

	/** All entries for a given portfolio — useful for bulk operations. */
	List<InvestmentEntry> findAllById_PortfolioName(String portfolioName);

	/** Check whether a ticker already exists in a specific portfolio. */
	boolean existsById_TickerAndId_PortfolioName(String ticker, String portfolioName);

	/** Fetch one entry by ticker + portfolio name. */
	Optional<InvestmentEntry> findById_TickerAndId_PortfolioName(String ticker, String portfolioName);
}