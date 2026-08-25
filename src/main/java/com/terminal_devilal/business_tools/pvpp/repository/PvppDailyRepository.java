package com.terminal_devilal.business_tools.pvpp.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.pvpp.entity.PvppDailyEntity;
import com.terminal_devilal.business_tools.pvpp.entity.PvppDailyId;

@Repository
public interface PvppDailyRepository extends JpaRepository<PvppDailyEntity, PvppDailyId> {

    List<PvppDailyEntity> findByDateBetweenOrderByDateDesc(LocalDate fromDate, LocalDate toDate);

    List<PvppDailyEntity> findByTickerAndDateBetweenOrderByDateDesc(String ticker, LocalDate fromDate, LocalDate toDate);

    List<PvppDailyEntity> findByDateAndTickerIn(LocalDate date, Set<String> tickers);
}
