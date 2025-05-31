package com.terminal_devilal.controllers.DataGathering.Model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(TickerDateId.class)
@Table(name = "atrt")
public class AverageTrueRange {

	@Id
	@Column(name = "ticker")
	private String ticker;
	
	@Id
	@Column(name = "date")
	private LocalDate date;

	@Column(name = "true_range")
	private double trueRange;

	public AverageTrueRange() {
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

	public double getTrueRange() {
		return trueRange;
	}

	public void setTrueRange(double trueRange) {
		this.trueRange = trueRange;
	}

	@Override
	public String toString() {
		return "AverageTrueRange [ticker=" + ticker + ", date=" + date + ", trueRange=" + trueRange + "]";
	}
	
    /**
     * Calculates the True Range (TR) for a single period.
     *
     * @param high Today's high price
     * @param low Today's low price
     * @param prevClose Previous day's closing price
     * @return True Range
     */
    public static double calculateTrueRange(double high, double low, double prevClose) {
        double range1 = high - low;
        double range2 = Math.abs(high - prevClose);
        double range3 = Math.abs(low - prevClose);

        return Math.max(range1, Math.max(range2, range3));
    }

}
