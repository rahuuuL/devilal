package com.terminal_devilal.core_business.portfolio_management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.terminal_devilal.core_business.portfolio_management.entity.Portfolio;

/**
 * PK is String (portfolio name). Was incorrectly Long in the original.
 */
public interface PortfolioDAO extends JpaRepository<Portfolio, String> {

    /** Fetch only active portfolios — used by the GET all endpoint. */
    List<Portfolio> findAllByActiveTrue();

    /** Find a single active portfolio by name. */
    Optional<Portfolio> findByNameAndActiveTrue(String name);

    /** Check existence by name (active or not) — used before creating to avoid duplicates. */
    boolean existsByName(String name);

    /**
     * Eagerly load investments in one query to avoid N+1 when returning the full response.
     */
    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.investments WHERE p.name = :name")
    Optional<Portfolio> findByNameWithInvestments(String name);

    /**
     * Eagerly load all active portfolios with their investments.
     */
    @Query("SELECT DISTINCT p FROM Portfolio p LEFT JOIN FETCH p.investments WHERE p.active = true")
    List<Portfolio> findAllActiveWithInvestments();
}