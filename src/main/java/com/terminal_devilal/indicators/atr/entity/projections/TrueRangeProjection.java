package com.terminal_devilal.indicators.atr.entity.projections;

import java.time.LocalDate;

public interface TrueRangeProjection {

	String getTicker();

	LocalDate getDate();

	double getTrueRange();
}