package com.terminal_devilal.controllers.DataGathering.Model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(TickerDateId.class)
@Table(name = "vwap")
public class RSI {

	@Id
	@Column(name = "ticker")
	private String ticker;
	
	@Id
	@Column(name = "date")
	private LocalDate date;

	@Column(name = "close_diff")
	private double closeDiff;

	public RSI(String ticker, LocalDate date, double closeDiff) {
		super();
		this.ticker = ticker;
		this.date = date;
		this.closeDiff = closeDiff;
	}

	public RSI() {
		super();
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public double getCloseDiff() {
		return closeDiff;
	}

	public void setCloseDiff(double closeDiff) {
		this.closeDiff = closeDiff;
	}

}
