package com.terminal_devilal.business_tools.ratio_analysis.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoGenerationHistoryEntity;
import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoGenerationHistoryId;
import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoGenerationStatus;

@Repository
public interface SharpeSortinoGenerationHistoryRepository
        extends JpaRepository<SharpeSortinoGenerationHistoryEntity, SharpeSortinoGenerationHistoryId> {

    List<SharpeSortinoGenerationHistoryEntity> findByDate(LocalDate date);

    List<SharpeSortinoGenerationHistoryEntity> findByDateAndStatus(LocalDate date,
            SharpeSortinoGenerationStatus status);
}
