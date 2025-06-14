package com.terminal_devilal.controllers.Functional.HeatMap.Model;

import java.time.LocalDate;

public record VolumeAnalysisDTO(String ticker, LocalDate date, long volume, double averageVolume,
		double volumePercentChange, double deliveryPercentage) {
}
