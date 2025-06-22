package com.terminal_devilal.controllers.Functional.Portfolio.DAO;

import org.springframework.data.jpa.repository.JpaRepository;

import com.terminal_devilal.controllers.Functional.Portfolio.Model.Portfolio;

public interface PortfolioDAO extends JpaRepository<Portfolio, Long> {
}