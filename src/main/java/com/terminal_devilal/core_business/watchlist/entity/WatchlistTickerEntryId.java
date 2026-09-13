package com.terminal_devilal.core_business.watchlist.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class WatchlistTickerEntryId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "watchlist_name", nullable = false)
    private String watchlistName;

    @Column(name = "ticker", nullable = false)
    private String ticker;

    public WatchlistTickerEntryId() {
    }

    public WatchlistTickerEntryId(String watchlistName, String ticker) {
        this.watchlistName = watchlistName;
        this.ticker = ticker;
    }

    public String getWatchlistName() { return watchlistName; }
    public String getTicker() { return ticker; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WatchlistTickerEntryId that)) return false;
        return Objects.equals(watchlistName, that.watchlistName) && Objects.equals(ticker, that.ticker);
    }

    @Override
    public int hashCode() { return Objects.hash(watchlistName, ticker); }
}
