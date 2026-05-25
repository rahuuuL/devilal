package com.terminal_devilal.core_business.portfolio_management.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "investment_entry")
public class InvestmentEntry {

	/**
	 * Composite PK: (ticker, portfolio_name). MapsId("portfolioName") wires the FK
	 * column into the embedded id so JPA doesn't need a separate FK column
	 * alongside the composite key.
	 */
	@EmbeddedId
	private InvestmentEntryId id;

	/** Buy price per unit at the time of entry. */
	@Column(name = "price", nullable = false)
	private double price;

	/** Number of units held. */
	@Column(name = "quantity", nullable = false)
	private int quantity;

	/**
	 * Date the ticker was added to the portfolio. Tells you when the position was
	 * opened.
	 */
	@Column(name = "added_date", nullable = false)
	private LocalDate addedDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("portfolioName")
	@JoinColumn(name = "portfolio_name", nullable = false)
	private Portfolio portfolio;

	// ── Constructors ────────────────────────────────────────────────────────

	public InvestmentEntry() {
		super();
	}

	public InvestmentEntry(String ticker, Portfolio portfolio, double price, int quantity, LocalDate addedDate) {
		this.id = new InvestmentEntryId(ticker, portfolio.getName());
		this.portfolio = portfolio;
		this.price = price;
		this.quantity = quantity;
		this.addedDate = addedDate;
	}

	// ── Convenience accessors (delegates to embedded id) ───────────────────

	public String getTicker() {
		return id != null ? id.getTicker() : null;
	}

	public String getPortfolioName() {
		return id != null ? id.getPortfolioName() : null;
	}

	// ── Getters & Setters ───────────────────────────────────────────────────

	public InvestmentEntryId getId() {
		return id;
	}

	public void setId(InvestmentEntryId id) {
		this.id = id;
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

	public Portfolio getPortfolio() {
		return portfolio;
	}

	public void setPortfolio(Portfolio portfolio) {
		this.portfolio = portfolio;
	}
}