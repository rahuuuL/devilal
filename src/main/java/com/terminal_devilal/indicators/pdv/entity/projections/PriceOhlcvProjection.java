package com.terminal_devilal.indicators.pdv.entity.projections;

import java.time.LocalDate;

public interface PriceOhlcvProjection {

	String getTicker();

	LocalDate getDate();

	double getOpen();

	double getClose();

	double getHigh();

	double getLow();

	double getDeliveryPercentage();

	long getVolume();

	double getVwap();

}
