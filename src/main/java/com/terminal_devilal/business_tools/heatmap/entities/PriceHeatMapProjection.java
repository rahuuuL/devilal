package com.terminal_devilal.business_tools.heatmap.entities;

public interface PriceHeatMapProjection {

	String getTicker();

	Double getOpen();

	Double getClose();

	Double getPercentChange();

}
