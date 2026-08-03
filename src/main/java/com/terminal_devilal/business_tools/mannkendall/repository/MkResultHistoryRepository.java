package com.terminal_devilal.business_tools.mannkendall.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryId;

@Repository
public interface MkResultHistoryRepository extends JpaRepository<MkResultHistoryEntity, MkResultHistoryId>,
                MkResultHistoryCustomRepository {

        List<MkResultHistoryEntity> findByDate(LocalDate date);

        List<MkResultHistoryEntity> findByDateAndDays(LocalDate date, Integer days);

        List<MkResultHistoryEntity> findByDateAndDaysAndTickerIn(LocalDate date, Integer days, Set<String> tickers);

        List<MkResultHistoryEntity> findByDateAndTickerIn(LocalDate date, Set<String> tickers);
}
