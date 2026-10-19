package com.terminal_devilal.business_tools.ratio_analysis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoConfigEntity;

@Repository
public interface SharpeSortinoConfigRepository extends JpaRepository<SharpeSortinoConfigEntity, Integer> {

    List<SharpeSortinoConfigEntity> findByEnabledTrueOrderByDaysAsc();
}
