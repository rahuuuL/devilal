package com.terminal_devilal.core_business.watchlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.terminal_devilal.core_business.watchlist.entity.WatchlistTickerEntry;
import com.terminal_devilal.core_business.watchlist.entity.WatchlistTickerEntryId;

public interface WatchlistTickerEntryDAO extends JpaRepository<WatchlistTickerEntry, WatchlistTickerEntryId> {
    List<WatchlistTickerEntry> findAllById_WatchlistName(String watchlistName);
    boolean existsById_WatchlistNameAndId_Ticker(String watchlistName, String ticker);
    Optional<WatchlistTickerEntry> findById_WatchlistNameAndId_Ticker(String watchlistName, String ticker);
}
