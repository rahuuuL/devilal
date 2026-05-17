package com.terminal_devilal.indicators.vwap.entities.projections;

import java.time.LocalDate;

public interface VwapProjection {
	String getTicker();

	LocalDate getDate();

	double getClosePrice();

	double getVwap();

	double getVwapProximity();
}
