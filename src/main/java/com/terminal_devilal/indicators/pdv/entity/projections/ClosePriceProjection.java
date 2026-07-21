package com.terminal_devilal.indicators.pdv.entity.projections;

import java.time.LocalDate;

public interface ClosePriceProjection {

	String getTicker();

	LocalDate getDate();

	double getClose();
}