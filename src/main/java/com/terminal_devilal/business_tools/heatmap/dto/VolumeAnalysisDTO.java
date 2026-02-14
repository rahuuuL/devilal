package com.terminal_devilal.business_tools.heatmap.dto;

import java.time.LocalDate;

public class VolumeAnalysisDTO {

	private String ticker;

	private LocalDate occurrenceDate;

	private double volumeCameIn;

	private double averageVolume;

	private double times;

	private double deliveryPercentage;

	public VolumeAnalysisDTO() {
		super();
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public LocalDate getOccurrenceDate() {
		return occurrenceDate;
	}

	public void setOccurrenceDate(LocalDate occurrenceDate) {
		this.occurrenceDate = occurrenceDate;
	}

	public double getVolumeCameIn() {
		return volumeCameIn;
	}

	public void setVolumeCameIn(double volumeCameIn) {
		this.volumeCameIn = volumeCameIn;
	}

	public double getAverageVolume() {
		return averageVolume;
	}

	public void setAverageVolume(double averageVolume) {
		this.averageVolume = averageVolume;
	}

	public double getTimes() {
		return times;
	}

	public void setTimes(double times) {
		this.times = times;
	}

	public double getDeliveryPercentage() {
		return deliveryPercentage;
	}

	public void setDeliveryPercentage(double deliveryPercentage) {
		this.deliveryPercentage = deliveryPercentage;
	}

}
