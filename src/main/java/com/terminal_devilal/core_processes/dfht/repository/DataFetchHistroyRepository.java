package com.terminal_devilal.core_processes.dfht.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.core_processes.dfht.entities.DataFetchEntity;

@Repository
public interface DataFetchHistroyRepository extends JpaRepository<DataFetchEntity, String> {

	@Query("SELECT DISTINCT d.ticker FROM DataFetchEntity d")
	List<String> findAllTickers();
}
