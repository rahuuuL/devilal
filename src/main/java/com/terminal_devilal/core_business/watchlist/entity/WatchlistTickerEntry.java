package com.terminal_devilal.core_business.watchlist.entity;

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
@Table(name = "watchlist_ticker_entry")
public class WatchlistTickerEntry {
    @EmbeddedId
    private WatchlistTickerEntryId id;

    @Column(name = "added_date", nullable = false)
    private LocalDate addedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("watchlistName")
    @JoinColumn(name = "watchlist_name", nullable = false)
    private Watchlist watchlist;

    public WatchlistTickerEntry() {
    }

    public WatchlistTickerEntry(String ticker, Watchlist watchlist, LocalDate addedDate) {
        this.id = new WatchlistTickerEntryId(watchlist.getName(), ticker);
        this.watchlist = watchlist;
        this.addedDate = addedDate;
    }

    public String getTicker() { return id == null ? null : id.getTicker(); }
    public LocalDate getAddedDate() { return addedDate; }
    public void setAddedDate(LocalDate addedDate) { this.addedDate = addedDate; }
}
