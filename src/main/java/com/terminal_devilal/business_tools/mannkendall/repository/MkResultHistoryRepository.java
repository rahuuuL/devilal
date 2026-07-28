package com.terminal_devilal.business_tools.mannkendall.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;
import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryId;

@Repository
public interface MkResultHistoryRepository extends JpaRepository<MkResultHistoryEntity, MkResultHistoryId> {

        @Modifying
        @Query("delete from MkResultHistoryEntity h where h.date = :date and h.days = :days")
        void deleteByDateAndDays(@Param("date") LocalDate date, @Param("days") Integer days);

    @Query("select h from MkResultHistoryEntity h where h.date = :date " +
            "and (:days is null or h.days = :days) " +
            "and (:tickers is null or h.ticker in :tickers)")
    List<MkResultHistoryEntity> findHistory(@Param("date") LocalDate date,
            @Param("days") Integer days,
            @Param("tickers") Collection<String> tickers);
}
