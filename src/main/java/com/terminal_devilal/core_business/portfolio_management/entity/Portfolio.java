package com.terminal_devilal.core_business.portfolio_management.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "portfolio")
public class Portfolio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name; // "Primary", "Secondary", etc.

	private String description;

	@OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL)
	private List<InvestmentEntry> investments = new ArrayList<>();

	// Constructors, Getters, Setters
	public Portfolio(Long id, String name, String description, List<InvestmentEntry> investments) {
		super();
		this.id = id;
		this.name = name;
		this.description = description;
		this.investments = investments;
	}

	public Portfolio() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<InvestmentEntry> getInvestments() {
		return investments;
	}

	public void setInvestments(List<InvestmentEntry> investments) {
		this.investments = investments;
	}

}
