package com.terminal_devilal.decision.repository;

import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DecisionIndicatorRepository extends JpaRepository<DecisionIndicatorEntity, String> {
    List<DecisionIndicatorEntity> findByEnabledTrueOrderByCategoryAscNameAsc();
    List<DecisionIndicatorEntity> findBySubjectTypeAndEnabledTrueOrderByCategoryAscNameAsc(String subjectType);
}
