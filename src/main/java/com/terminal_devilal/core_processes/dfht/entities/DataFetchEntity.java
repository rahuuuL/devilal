package com.terminal_devilal.core_processes.dfht.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dfht")
public class DataFetchEntity {

	@Id
	@Column(name = "ticker")
	private String ticker;

	@Column(name = "pdvt_last_date")
	private LocalDate pdvtLastDate;
	
	public DataFetchEntity() {
		super();
	}

	public DataFetchEntity(String ticker, LocalDate pdvtLastDate) {
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
