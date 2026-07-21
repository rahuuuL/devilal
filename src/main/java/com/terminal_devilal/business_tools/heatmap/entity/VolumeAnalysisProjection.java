package com.terminal_devilal.business_tools.heatmap.entity;

import java.time.LocalDate;

public interface VolumeAnalysisProjection {
	String getTicker();

	LocalDate getDate();

	Double getVolume();

	Double getAvgVolume();

	Double getTimes();

	Double getDeliveryPercentage();
}
