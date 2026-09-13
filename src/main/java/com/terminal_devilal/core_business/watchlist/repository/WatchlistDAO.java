package com.terminal_devilal.core_business.watchlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.terminal_devilal.core_business.watchlist.entity.Watchlist;

public interface WatchlistDAO extends JpaRepository<Watchlist, String> {
    @Query("SELECT DISTINCT w FROM Watchlist w LEFT JOIN FETCH w.entries WHERE w.active = true")
    List<Watchlist> findAllActiveWithEntries();

    @Query("SELECT w FROM Watchlist w LEFT JOIN FETCH w.entries WHERE w.name = :name")
    Optional<Watchlist> findByNameWithEntries(String name);

    Optional<Watchlist> findByNameAndActiveTrue(String name);
    boolean existsByName(String name);
}
