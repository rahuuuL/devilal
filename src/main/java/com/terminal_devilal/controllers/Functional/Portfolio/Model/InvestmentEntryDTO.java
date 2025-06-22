package com.terminal_devilal.controllers.Functional.Portfolio.Model;

import java.time.LocalDate;

public record InvestmentEntryDTO(Long id, // Optional (for update)
		String ticker, double price, int quantity, LocalDate addedDate, double riskFreeRate) {
}
