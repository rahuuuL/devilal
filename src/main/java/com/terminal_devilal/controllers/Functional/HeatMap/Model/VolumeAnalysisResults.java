package com.terminal_devilal.controllers.Functional.HeatMap.Model;

import java.time.LocalDate;

public interface VolumeAnalysisResults {
	String getTicker();

	LocalDate getDate();

	Double getVolume();

	Double getAvgVolume();

	Double getTimes();

	Double getDeliveryPercentage();
}
