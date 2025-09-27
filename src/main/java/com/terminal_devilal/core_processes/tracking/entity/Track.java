package com.terminal_devilal.core_processes.tracking.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "track")
public class Track {

	@Id
	@Column(name = "track_id", nullable = false, updatable = false)
	private String trackId;

	@Column(nullable = false)
	private String ticker;

	@Column(name = "added_date", nullable = false)
	private LocalDate addedDate;

	@Column(name = "price_on_date_added", nullable = false)
	private double priceOnAddedDate;

	@Column(name = "drop_date")
	private LocalDate dropDate;

	@Column(name = "active_tracking", nullable = false)
	private boolean activeTracking = true;

	@ElementCollection
	@CollectionTable(name = "track_rule_mapping", joinColumns = @JoinColumn(name = "track_id"))
	@Column(name = "rules")
	private List<Integer> rules = new ArrayList<>();

	@Column(name = "last_close")
	private double lastClose;

	// Constructors
	public Track() {
		this.trackId = UUID.randomUUID().toString();
	}

	public Track(String ticker, LocalDate addedDate, double priceOnAddedDate, double lastClose,List<Integer> rules) {
		this.trackId = UUID.randomUUID().toString();
		this.ticker = ticker;
		this.addedDate = addedDate;
		this.priceOnAddedDate = priceOnAddedDate; 
		this.activeTracking = true;
		this.lastClose = lastClose;
		this.rules = rules;
	}

	// Getters and Setters
	public String getTrackId() {
		return trackId;
	}

	// No setter for trackId to keep it immutable

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public LocalDate getAddedDate() {
		return addedDate;
	}

	public void setAddedDate(LocalDate addedDate) {
		this.addedDate = addedDate;
	}

	public LocalDate getDropDate() {
		return dropDate;
	}

	public void setDropDate(LocalDate dropDate) {
		this.dropDate = dropDate;
	}

	public double getPriceOnAddedDate() {
		return priceOnAddedDate;
	}

	public void setPriceOnAddedDate(double priceOnAddedDate) {
		this.priceOnAddedDate = priceOnAddedDate;
	}

	public boolean isActiveTracking() {
		return activeTracking;
	}

	public void setActiveTracking(boolean activeTracking) {
		this.activeTracking = activeTracking;
	}

	public List<Integer> getRuleIds() {
		return rules;
	}

	public void setRuleIds(List<Integer> rules) {
		this.rules = rules;
	}

	public double getLastClose() {
		return lastClose;
	}

	public void setLastClose(double lastClose) {
		this.lastClose = lastClose;
	}

}
