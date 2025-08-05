package com.terminal_devilal.controllers.DataGathering.Enums;

public enum PriceVolumeDeliveryColumn {
	VOLUME("volume"), HIGH("high"), LOW("low"), CLOSE("close");

	private final String columnName;

	PriceVolumeDeliveryColumn(String columnName) {
		this.columnName = columnName;
	}

	public String getColumnName() {
		return columnName;
	}
}
