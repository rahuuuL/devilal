package com.terminal_devilal.core_business.portfolio_management.dto;

import java.time.LocalDate;

public class InvestmentEntryDTO {

	/**
	 * Used for POST /portfolio/{name}/entries — add a new ticker to a portfolio.
	 * addedDate is optional; defaults to today in the service if not provided.
	 */
	public record AddEntryRequest(String ticker, double price, int quantity, LocalDate addedDate) {
	}

	/**
	 * Used for PATCH /portfolio/{name}/entries/{ticker} — update price, quantity,
	 * or riskFreeRate on an existing entry. Only supplied fields are changed.
	 * ticker and addedDate are immutable after creation.
	 */
	public record UpdateEntryRequest(double price, int quantity, double riskFreeRate) {
	}

	/**
	 * Response shape for a single investment entry.
	 */
	public record EntryResponse(String ticker, double price, int quantity, LocalDate addedDate) {
	}
}