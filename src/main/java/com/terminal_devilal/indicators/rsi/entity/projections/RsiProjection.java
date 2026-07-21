package com.terminal_devilal.indicators.rsi.entity.projections;

import java.time.LocalDate;

public interface RsiProjection {
	String getTicker();

	LocalDate getDate();

	double getCloseDiff();

	double getFourteenDaysRsi();

	double getTwentyOneDaysRsi();
}
