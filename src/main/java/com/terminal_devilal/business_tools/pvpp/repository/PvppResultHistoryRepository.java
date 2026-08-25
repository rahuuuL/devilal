package com.terminal_devilal.business_tools.pvpp.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.pvpp.entity.PvppResultHistoryEntity;
import com.terminal_devilal.business_tools.pvpp.entity.PvppResultHistoryId;

@Repository
public interface PvppResultHistoryRepository extends JpaRepository<PvppResultHistoryEntity, PvppResultHistoryId> {

    List<PvppResultHistoryEntity> findByDateBetweenOrderByDateDesc(LocalDate fromDate, LocalDate toDate);

    List<PvppResultHistoryEntity> findByDateBetweenAndDaysOrderByDateDesc(LocalDate fromDate, LocalDate toDate, Integer days);

    List<PvppResultHistoryEntity> findByDateBetweenAndDaysAndTickerInOrderByDateDesc(LocalDate fromDate, LocalDate toDate,
            Integer days, Set<String> tickers);

    List<PvppResultHistoryEntity> findByDateBetweenAndTickerInOrderByDateDesc(LocalDate fromDate, LocalDate toDate,
            Set<String> tickers);
}
