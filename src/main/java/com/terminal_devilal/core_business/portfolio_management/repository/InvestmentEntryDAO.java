package com.terminal_devilal.core_business.portfolio_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.terminal_devilal.core_business.portfolio_management.entity.InvestmentEntry;

public interface InvestmentEntryDAO extends JpaRepository<InvestmentEntry, Long> {
}