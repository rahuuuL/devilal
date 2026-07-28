package com.terminal_devilal.business_tools.mannkendall.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.mannkendall.entity.MkConfigEntity;

@Repository
public interface MkConfigRepository extends JpaRepository<MkConfigEntity, Integer> {

    List<MkConfigEntity> findByEnabledTrueOrderByDaysAsc();
}
