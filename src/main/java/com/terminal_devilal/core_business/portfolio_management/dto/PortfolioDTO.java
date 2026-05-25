package com.terminal_devilal.core_business.portfolio_management.dto;

import java.time.LocalDate;
import java.util.List;

// ── Request DTOs ────────────────────────────────────────────────────────────

/**
 * Used for POST /portfolio — create a new portfolio. investments list is
 * optional on creation; entries can be added later.
 */
public class PortfolioDTO {

	public record CreatePortfolioRequest(String name) {
	}

	/**
	 * Used for PATCH /portfolio/{name} — update description only. Name is immutable
	 * (it is the PK).
	 */
	public record UpdatePortfolioRequest(String description) {
	}

	// ── Response DTO ────────────────────────────────────────────────────────

	/**
	 * Full portfolio response including all active investment entries.
	 */
	public record PortfolioResponse(String name, boolean active, LocalDate lastUpdatedDate,
			List<InvestmentEntryDTO.EntryResponse> investments) {
	}
}