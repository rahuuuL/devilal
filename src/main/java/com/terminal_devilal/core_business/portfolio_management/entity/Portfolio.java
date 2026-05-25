package com.terminal_devilal.core_business.portfolio_management.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "portfolio")
public class Portfolio {

	/**
	 * Portfolio name is the natural business key and primary key. e.g. "Primary",
	 * "Retirement", "Trading"
	 */
	@Id
	@Column(name = "name", nullable = false, unique = true)
	private String name;

	/**
	 * Soft-delete flag. When false the portfolio is considered deleted. Hard
	 * deletes are never done on portfolios — only on investment entries.
	 */
	@Column(name = "active", nullable = false)
	private boolean active = true;

	/**
	 * Updated on every action: create, deactivate, add entry, remove entry, update
	 * entry. Lets consumers know the timestamp of the last meaningful change.
	 */
	@Column(name = "last_updated_date", nullable = false)
	private LocalDate lastUpdatedDate;

	/**
	 * orphanRemoval = true ensures that when an InvestmentEntry is removed from
	 * this list it is also deleted from the database automatically.
	 */
	@OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<InvestmentEntry> investments = new ArrayList<>();

	// ── Constructors ────────────────────────────────────────────────────────

	public Portfolio() {
		super();
	}

	public Portfolio(String name) {
		this.name = name;
		this.active = true;
		this.lastUpdatedDate = LocalDate.now();
	}

	// ── Getters & Setters ───────────────────────────────────────────────────

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public LocalDate getLastUpdatedDate() {
		return lastUpdatedDate;
	}

	public void setLastUpdatedDate(LocalDate lastUpdatedDate) {
		this.lastUpdatedDate = lastUpdatedDate;
	}

	public List<InvestmentEntry> getInvestments() {
		return investments;
	}

	public void setInvestments(List<InvestmentEntry> investments) {
		this.investments = investments;
	}
}