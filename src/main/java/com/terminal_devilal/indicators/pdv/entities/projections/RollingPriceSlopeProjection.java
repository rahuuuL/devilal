package com.terminal_devilal.indicators.pdv.entities.projections;

import java.time.LocalDate;

public interface RollingPriceSlopeProjection {

	String getTicker();

	LocalDate getDate();

	Double getPrice();

}
