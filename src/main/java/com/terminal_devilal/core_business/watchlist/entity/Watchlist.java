package com.terminal_devilal.core_business.watchlist.entity;

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
@Table(name = "watchlist")
public class Watchlist {
    @Id
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "last_updated_date", nullable = false)
    private LocalDate lastUpdatedDate;

    @OneToMany(mappedBy = "watchlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WatchlistTickerEntry> entries = new ArrayList<>();

    public Watchlist() {
    }

    public Watchlist(String name) {
        this.name = name;
        this.lastUpdatedDate = LocalDate.now();
    }

    public String getName() { return name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDate getLastUpdatedDate() { return lastUpdatedDate; }
    public void setLastUpdatedDate(LocalDate lastUpdatedDate) { this.lastUpdatedDate = lastUpdatedDate; }
    public List<WatchlistTickerEntry> getEntries() { return entries; }
}
