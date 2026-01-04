package com.terminal_devilal.indicators.pdv.repository.projection;

import java.time.LocalDate;

public interface ClosePriceProjection {
	LocalDate getDate();

	Double getClose();

}
