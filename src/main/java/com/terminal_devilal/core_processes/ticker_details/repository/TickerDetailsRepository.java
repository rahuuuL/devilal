package com.terminal_devilal.core_processes.ticker_details.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.core_processes.sync_data.entity.TickerIndustryInfo;
import com.terminal_devilal.core_processes.ticker_details.model.TickerDetailsResponse;

@Repository
public interface TickerDetailsRepository extends JpaRepository<TickerIndustryInfo, String> {

	@Query(name = "TickerIndustryInfo.fetchAllTickerDetailsFast", nativeQuery = true)
	List<TickerDetailsResponse> fetchAllTickerDetailsFast();

}
