package com.terminal_devilal.business_tools.pvpp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.pvpp.entity.PvppConfigEntity;

@Repository
public interface PvppConfigRepository extends JpaRepository<PvppConfigEntity, Integer> {

    List<PvppConfigEntity> findByEnabledTrueOrderByDaysAsc();
}
