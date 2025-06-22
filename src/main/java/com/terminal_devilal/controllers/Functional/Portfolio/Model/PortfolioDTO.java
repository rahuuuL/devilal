package com.terminal_devilal.controllers.Functional.Portfolio.Model;

import java.util.List;

public record PortfolioDTO(String name, String description, List<InvestmentEntryDTO> investments) {
}