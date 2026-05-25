package com.terminal_devilal.indicators.atr.entities.projections;

import java.time.LocalDate;

public interface TrueRangeProjection {

	String getTicker();

	LocalDate getDate();

	double getTrueRange();
}