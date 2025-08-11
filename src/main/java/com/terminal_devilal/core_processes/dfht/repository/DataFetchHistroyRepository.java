package com.terminal_devilal.core_processes.dfht.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.core_processes.dfht.entities.DataFetchEntity;

@Repository
public interface DataFetchHistroyRepository extends JpaRepository<DataFetchEntity, String> {

}
