package com.terminal_devilal.core_business.portfolio_management.dto;

import java.time.LocalDate;

public record InvestmentEntryDTO(Long id, // Optional (for update)
		String ticker, double price, int quantity, LocalDate addedDate, double riskFreeRate) {
}
