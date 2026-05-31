package com.terminal_devilal.indicators.pdv.entities.projections;

import java.time.LocalDate;

public interface ClosePriceProjection {

	String getTicker();

	LocalDate getDate();

	double getClose();
}