package com.terminal_devilal.business_tools.ratio_analysis.dto;

import java.io.Serializable;
import java.time.LocalDate;

public class Series implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	LocalDate date;
	double ratio;

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public double getRatio() {
		return ratio;
	}

	public void setRatio(double ratio) {
		this.ratio = ratio;
	}

	public Series(LocalDate date, double ratio) {
		super();
		this.date = date;
		this.ratio = ratio;
	}

}