package com.terminal_devilal.indicators.pdv.entity.projections;

import java.time.LocalDate;

public interface ConsistentVolumeProjection {

	String getTicker();

	LocalDate getDate();

	Long getVolume();

}