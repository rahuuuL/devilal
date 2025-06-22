package com.terminal_devilal.controllers.Functional.Drawdown.Model;

import java.time.LocalDate;

public record DrawdownDTO(LocalDate peakDate, double peakPrice, LocalDate troughDate, double troughPrice,
		double drawdownPercent, int daysToRecover) {
}
