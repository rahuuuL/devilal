package com.terminal_devilal.decision.repository;

import com.terminal_devilal.decision.entity.DecisionOutputVariableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DecisionOutputVariableRepository extends JpaRepository<DecisionOutputVariableEntity, Long> {
    List<DecisionOutputVariableEntity> findByProfileIdOrderByCode(Long profileId);
    Optional<DecisionOutputVariableEntity> findByProfileIdAndCode(Long profileId, String code);
}
