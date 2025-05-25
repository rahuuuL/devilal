package com.terminal_devilal.controllers.DataGathering.Model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dfht")
public class DataFetchHistroy {

	@Id
	@Column(name = "ticker")
	private String ticker;

	@Column(name = "pdvt_last_date")
	private LocalDate pdvtLastDate;
	
	public DataFetchHistroy() {
		super();
	}

	public DataFetchHistroy(String ticker, LocalDate pdvtLastDate) {
		this.ticker = ticker;
		this.pdvtLastDate = pdvtLastDate;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public LocalDate getPdvtLastDate() {
		return pdvtLastDate;
	}

	public void setPdvtLastDate(LocalDate pdvtLastDate) {
		this.pdvtLastDate = pdvtLastDate;
	}

	

}
