package com.terminal_devilal.business_tools.mannkendall.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.mannkendall.entity.MkGenerationHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.entity.MkGenerationHistoryId;
import com.terminal_devilal.business_tools.mannkendall.entity.MkGenerationStatus;

@Repository
public interface MkGenerationHistoryRepository extends JpaRepository<MkGenerationHistoryEntity, MkGenerationHistoryId> {

    List<MkGenerationHistoryEntity> findByDate(LocalDate date);

    List<MkGenerationHistoryEntity> findByDateAndStatus(LocalDate date, MkGenerationStatus status);
}
