package com.terminal_devilal.indicators.rsi.entities;

import java.time.LocalDate;

import com.terminal_devilal.indicators.common_entities.TickerDateId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(TickerDateId.class)
@Table(name = "rsi")
public class RSIEntity {

	@Id
	@Column(name = "ticker")
	private String ticker;

	@Id
	@Column(name = "date")
	private LocalDate date;

	@Column(name = "close_diff")
	private double closeDiff;

	@Column(name = "14_days_rsi")
	private double FourtheenDaysRSI;

	@Column(name = "21_days_rsi")
	private double TweentyOneDaysRSI;

	public RSIEntity(String ticker, LocalDate date, double closeDiff, double fourtheenDaysRSI,
			double tweentyOneDaysRSI) {
		super();
		this.ticker = ticker;
		this.date = date;
		this.closeDiff = closeDiff;
		FourtheenDaysRSI = fourtheenDaysRSI;
		TweentyOneDaysRSI = tweentyOneDaysRSI;
	}

	public RSIEntity() {
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

	public double getFourtheenDaysRSI() {
		return FourtheenDaysRSI;
	}

	public void setFourtheenDaysRSI(double fourtheenDaysRSI) {
		FourtheenDaysRSI = fourtheenDaysRSI;
	}

	public double getTweentyOneDaysRSI() {
		return TweentyOneDaysRSI;
	}

	public void setTweentyOneDaysRSI(double tweentyOneDaysRSI) {
		TweentyOneDaysRSI = tweentyOneDaysRSI;
	}

	@Override
	public String toString() {
		return "RSI [ticker=" + ticker + ", date=" + date + ", closeDiff=" + closeDiff + ", FourtheenDaysRSI="
				+ FourtheenDaysRSI + ", TweentyOneDaysRSI=" + TweentyOneDaysRSI + "]";
	}

}
