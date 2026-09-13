package com.terminal_devilal.core_business.watchlist.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.core_business.watchlist.dto.WatchlistDTO;
import com.terminal_devilal.core_business.watchlist.entity.Watchlist;
import com.terminal_devilal.core_business.watchlist.entity.WatchlistTickerEntry;
import com.terminal_devilal.core_business.watchlist.repository.WatchlistDAO;
import com.terminal_devilal.core_business.watchlist.repository.WatchlistTickerEntryDAO;

@Service
@Transactional
public class WatchlistService {
    private final WatchlistDAO watchlistDAO;
    private final WatchlistTickerEntryDAO entryDAO;

    public WatchlistService(WatchlistDAO watchlistDAO, WatchlistTickerEntryDAO entryDAO) {
        this.watchlistDAO = watchlistDAO;
        this.entryDAO = entryDAO;
    }

    @Transactional(readOnly = true)
    public List<WatchlistDTO.WatchlistResponse> getAllWatchlists() {
        return watchlistDAO.findAllActiveWithEntries().stream().map(this::toResponse).toList();
    }

    public WatchlistDTO.WatchlistResponse createWatchlist(WatchlistDTO.CreateWatchlistRequest request) {
        requireText(request.name(), "Watchlist name");
        if (watchlistDAO.existsByName(request.name())) {
            throw new IllegalArgumentException("Watchlist with name '" + request.name() + "' already exists.");
        }
        return toResponse(watchlistDAO.save(new Watchlist(request.name())));
    }

    public WatchlistDTO.WatchlistResponse addTicker(String watchlistName, WatchlistDTO.AddTickerRequest request) {
        Watchlist watchlist = findActiveWatchlist(watchlistName);
        requireText(request.ticker(), "Ticker");
        if (entryDAO.existsById_WatchlistNameAndId_Ticker(watchlistName, request.ticker())) {
            throw new IllegalArgumentException("Ticker '" + request.ticker() + "' already exists in watchlist '" + watchlistName + "'.");
        }
        WatchlistTickerEntry entry = new WatchlistTickerEntry(request.ticker(), watchlist,
                request.addedDate() == null ? LocalDate.now() : request.addedDate());
        watchlist.getEntries().add(entry);
        watchlist.setLastUpdatedDate(LocalDate.now());
        return toResponse(watchlistDAO.save(watchlist));
    }

    public WatchlistDTO.WatchlistResponse removeTicker(String watchlistName, String ticker) {
        Watchlist watchlist = findActiveWatchlist(watchlistName);
        WatchlistTickerEntry entry = entryDAO.findById_WatchlistNameAndId_Ticker(watchlistName, ticker)
                .orElseThrow(() -> new IllegalArgumentException("Ticker '" + ticker + "' not found in watchlist '" + watchlistName + "'."));
        watchlist.getEntries().remove(entry);
        watchlist.setLastUpdatedDate(LocalDate.now());
        return toResponse(watchlistDAO.save(watchlist));
    }

    public WatchlistDTO.WatchlistResponse updateTicker(String watchlistName, String ticker, WatchlistDTO.UpdateTickerRequest request) {
        Watchlist watchlist = findActiveWatchlist(watchlistName);
        WatchlistTickerEntry entry = entryDAO.findById_WatchlistNameAndId_Ticker(watchlistName, ticker)
                .orElseThrow(() -> new IllegalArgumentException("Ticker '" + ticker + "' not found in watchlist '" + watchlistName + "'."));
        if (request.addedDate() != null) entry.setAddedDate(request.addedDate());
        watchlist.setLastUpdatedDate(LocalDate.now());
        entryDAO.save(entry);
        return toResponse(watchlistDAO.save(watchlist));
    }

    public WatchlistDTO.WatchlistResponse deactivateWatchlist(String name) {
        Watchlist watchlist = findActiveWatchlist(name);
        watchlist.setActive(false);
        watchlist.setLastUpdatedDate(LocalDate.now());
        return toResponse(watchlistDAO.save(watchlist));
    }

    @Transactional(readOnly = true)
    public List<String> getTickers(String name) {
        return watchlistDAO.findByNameWithEntries(name)
                .filter(Watchlist::isActive)
                .map(watchlist -> watchlist.getEntries().stream().map(WatchlistTickerEntry::getTicker).toList())
                .orElseThrow(() -> new IllegalArgumentException("Active watchlist '" + name + "' not found."));
    }

    private Watchlist findActiveWatchlist(String name) {
        return watchlistDAO.findByNameAndActiveTrue(name)
                .orElseThrow(() -> new IllegalArgumentException("Active watchlist '" + name + "' not found."));
    }

    private WatchlistDTO.WatchlistResponse toResponse(Watchlist watchlist) {
        return new WatchlistDTO.WatchlistResponse(watchlist.getName(), watchlist.isActive(), watchlist.getLastUpdatedDate(),
                watchlist.getEntries().stream().map(entry -> new WatchlistDTO.TickerResponse(entry.getTicker(), entry.getAddedDate())).toList());
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
    }
}
