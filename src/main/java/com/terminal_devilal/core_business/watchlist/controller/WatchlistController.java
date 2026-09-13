package com.terminal_devilal.core_business.watchlist.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.core_business.watchlist.dto.WatchlistDTO;
import com.terminal_devilal.core_business.watchlist.service.WatchlistService;

@RestController
@RequestMapping("/api/devilal/watchlist")
public class WatchlistController {
    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<WatchlistDTO.WatchlistResponse> getAllWatchlists() {
        return watchlistService.getAllWatchlists();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistDTO.WatchlistResponse createWatchlist(@RequestBody WatchlistDTO.CreateWatchlistRequest request) {
        return watchlistService.createWatchlist(request);
    }

    @PostMapping("/{name}/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistDTO.WatchlistResponse addTicker(@PathVariable String name, @RequestBody WatchlistDTO.AddTickerRequest request) {
        return watchlistService.addTicker(name, request);
    }

    @DeleteMapping("/{name}/entries/{ticker}")
    public WatchlistDTO.WatchlistResponse removeTicker(@PathVariable String name, @PathVariable String ticker) {
        return watchlistService.removeTicker(name, ticker);
    }

    @PatchMapping("/{name}/entries/{ticker}")
    public WatchlistDTO.WatchlistResponse updateTicker(@PathVariable String name, @PathVariable String ticker, @RequestBody WatchlistDTO.UpdateTickerRequest request) {
        return watchlistService.updateTicker(name, ticker, request);
    }

    @DeleteMapping("/{name}")
    public WatchlistDTO.WatchlistResponse deactivateWatchlist(@PathVariable String name) {
        return watchlistService.deactivateWatchlist(name);
    }
}
