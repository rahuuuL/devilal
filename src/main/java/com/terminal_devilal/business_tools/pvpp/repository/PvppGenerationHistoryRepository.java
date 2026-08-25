package com.terminal_devilal.business_tools.pvpp.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.pvpp.entity.PvppGenerationHistoryEntity;
import com.terminal_devilal.business_tools.pvpp.entity.PvppGenerationHistoryId;
import com.terminal_devilal.business_tools.pvpp.entity.PvppGenerationStatus;

@Repository
public interface PvppGenerationHistoryRepository extends JpaRepository<PvppGenerationHistoryEntity, PvppGenerationHistoryId> {

    List<PvppGenerationHistoryEntity> findByDate(LocalDate date);

    List<PvppGenerationHistoryEntity> findByDateAndStatus(LocalDate date, PvppGenerationStatus status);
}
