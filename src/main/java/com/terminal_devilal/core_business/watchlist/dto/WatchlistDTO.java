package com.terminal_devilal.core_business.watchlist.dto;

import java.time.LocalDate;
import java.util.List;

public final class WatchlistDTO {
    private WatchlistDTO() {
    }

    public record CreateWatchlistRequest(String name) {}
    public record AddTickerRequest(String ticker, LocalDate addedDate) {}
    public record UpdateTickerRequest(LocalDate addedDate) {}
    public record TickerResponse(String ticker, LocalDate addedDate) {}
    public record WatchlistResponse(String name, boolean active, LocalDate lastUpdatedDate, List<TickerResponse> tickers) {}
}
