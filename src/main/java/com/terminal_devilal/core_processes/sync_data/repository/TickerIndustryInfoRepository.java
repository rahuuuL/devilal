package com.terminal_devilal.core_processes.sync_data.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.terminal_devilal.core_processes.sync_data.entity.TickerIndustryInfo;

public interface TickerIndustryInfoRepository extends JpaRepository<TickerIndustryInfo, String> {
}