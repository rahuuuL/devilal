package com.terminal_devilal.business_tools.heatmap.entity;

public interface PriceHeatMapProjection {

	String getTicker();

	Double getOpen();

	Double getClose();

	Double getPercentChange();

}
