package com.terminal_devilal.core_business.portfolio_management.dto;

import java.util.List;

public record PortfolioDTO(String name, String description, List<InvestmentEntryDTO> investments) {
}