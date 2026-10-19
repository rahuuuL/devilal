package com.terminal_devilal.business_tools.ratio_analysis.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoResultHistoryEntity;
import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoResultHistoryId;

@Repository
public interface SharpeSortinoResultHistoryRepository
        extends JpaRepository<SharpeSortinoResultHistoryEntity, SharpeSortinoResultHistoryId> {

    List<SharpeSortinoResultHistoryEntity> findByDateBetweenAndRollingDaysOrderByDateDesc(
            LocalDate fromDate, LocalDate toDate, Integer rollingDays);

    List<SharpeSortinoResultHistoryEntity> findByDateBetweenAndRollingDaysAndTickerInOrderByDateDesc(
            LocalDate fromDate, LocalDate toDate, Integer rollingDays, Set<String> tickers);
}
