package com.terminal_devilal.controllers.Functional.Portfolio.DAO;

import org.springframework.data.jpa.repository.JpaRepository;

import com.terminal_devilal.controllers.Functional.Portfolio.Model.InvestmentEntry;

public interface InvestmentEntryDAO extends JpaRepository<InvestmentEntry, Long> {
}