package com.terminal_devilal.indicators.pdv.entities.projections;

import java.time.LocalDate;

public interface ConsistentVolumeProjection {

	String getTicker();

	LocalDate getDate();

	Long getVolume();

}