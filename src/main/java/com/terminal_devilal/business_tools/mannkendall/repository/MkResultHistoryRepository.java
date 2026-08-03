package com.terminal_devilal.business_tools.mannkendall.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import io.micrometer.core.annotation.Timed;

import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryId;

@Repository
public interface MkResultHistoryRepository extends JpaRepository<MkResultHistoryEntity, MkResultHistoryId> {

        @Modifying
        @Query("delete from MkResultHistoryEntity h where h.date = :date and h.days = :days")
        void deleteByDateAndDays(@Param("date") LocalDate date, @Param("days") Integer days);

        List<MkResultHistoryEntity> findByDate(LocalDate date);

        List<MkResultHistoryEntity> findByDateAndDays(LocalDate date, Integer days);

        List<MkResultHistoryEntity> findByDateAndDaysAndTickerIn(LocalDate date, Integer days, Set<String> tickers);

        List<MkResultHistoryEntity> findByDateAndTickerIn(LocalDate date, Set<String> tickers);
}
