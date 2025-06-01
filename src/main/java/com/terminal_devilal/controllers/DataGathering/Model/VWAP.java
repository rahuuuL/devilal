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
public class VWAP {

	@Id
	@Column(name = "ticker")
	private String ticker;

	@Id
	@Column(name = "date")
	private LocalDate date;

	@Column(name = "close_price")
	private double closePrice;

	@Column(name = "vwap")
	private double vwap;

	@Column(name = "vwap_proximity")
	private double vwapProximity;

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

	public double getClosePrice() {
		return closePrice;
	}

	public void setClosePrice(double closePrice) {
		this.closePrice = closePrice;
	}

	public double getVwap() {
		return vwap;
	}

	public void setVwap(double vwap) {
		this.vwap = vwap;
	}

	public double getVwapProximity() {
		return vwapProximity;
	}

	public void setVwapProximity(double vwapProximity) {
		this.vwapProximity = vwapProximity;
	}

	public VWAP() {
		super();
	}

	public VWAP(String ticker, LocalDate date, double closePrice, double vwap, double vwapProximity) {
		super();
		this.ticker = ticker;
		this.date = date;
		this.closePrice = closePrice;
		this.vwap = vwap;
		this.vwapProximity = vwapProximity;
	}

}
