package com.terminal_devilal.controllers.Functional.Portfolio.Model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "investment_entry")
public class InvestmentEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String ticker;

	private double price;

	private int quantity;

	private LocalDate addedDate;

	private double riskFreeRate;

	@ManyToOne
	@JoinColumn(name = "portfolio", nullable = false)
	private Portfolio portfolio;

	// Constructors, Getters, Setters
	public InvestmentEntry(Long id, String ticker, double price, int quantity, LocalDate addedDate, double riskFreeRate,
			Portfolio portfolio) {
		super();
		this.id = id;
		this.ticker = ticker;
		this.price = price;
		this.quantity = quantity;
		this.addedDate = addedDate;
		this.riskFreeRate = riskFreeRate;
		this.portfolio = portfolio;
	}

	public InvestmentEntry() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public LocalDate getAddedDate() {
		return addedDate;
	}

	public void setAddedDate(LocalDate addedDate) {
		this.addedDate = addedDate;
	}

	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	public void setRiskFreeRate(double riskFreeRate) {
		this.riskFreeRate = riskFreeRate;
	}

	public Portfolio getPortfolio() {
		return portfolio;
	}

	public void setPortfolio(Portfolio portfolio) {
		this.portfolio = portfolio;
	}

}
