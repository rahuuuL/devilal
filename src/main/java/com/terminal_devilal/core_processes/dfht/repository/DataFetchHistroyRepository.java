package com.terminal_devilal.core_processes.dfht.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.core_processes.dfht.entities.DataFetchEntity;

@Repository
public interface DataFetchHistroyRepository extends JpaRepository<DataFetchEntity, String> {

	@Query("SELECT DISTINCT d.ticker FROM DataFetchEntity d")
	List<String> findAllTickers();

	@Modifying
	@Query("UPDATE DataFetchEntity d SET d.pdvtLastDate = :date WHERE d.ticker = :ticker")
	void updateLastDate(@Param("ticker") String ticker, @Param("date") LocalDate date);
}
