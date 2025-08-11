package com.terminal_devilal.business_tools.drawdown.dto;

import java.time.LocalDate;

public record DrawdownDTO(LocalDate peakDate, double peakPrice, LocalDate troughDate, double troughPrice,
		double drawdownPercent, int daysToRecover) {
}
