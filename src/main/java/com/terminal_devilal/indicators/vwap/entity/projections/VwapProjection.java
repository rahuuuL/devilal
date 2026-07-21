package com.terminal_devilal.indicators.vwap.entity.projections;

import java.time.LocalDate;

public interface VwapProjection {
	String getTicker();

	LocalDate getDate();

	double getClosePrice();

	double getVwap();

	double getVwapProximity();
}
