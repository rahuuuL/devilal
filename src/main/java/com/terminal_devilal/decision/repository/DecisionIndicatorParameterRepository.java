package com.terminal_devilal.decision.repository;

import com.terminal_devilal.decision.entity.DecisionIndicatorParameterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DecisionIndicatorParameterRepository extends JpaRepository<DecisionIndicatorParameterEntity, Long> {
    List<DecisionIndicatorParameterEntity> findByIndicatorCodeOrderBySequenceNoAsc(String indicatorCode);
    List<DecisionIndicatorParameterEntity> findByIndicatorCodeAndEnabledTrueOrderBySequenceNoAsc(String indicatorCode);
    Optional<DecisionIndicatorParameterEntity> findByIndicatorCodeAndParameterCode(String indicatorCode, String parameterCode);
}
