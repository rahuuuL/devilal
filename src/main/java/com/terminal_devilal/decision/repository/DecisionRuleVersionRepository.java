package com.terminal_devilal.decision.repository;

import com.terminal_devilal.decision.entity.DecisionRuleVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DecisionRuleVersionRepository extends JpaRepository<DecisionRuleVersionEntity, Long> {
    List<DecisionRuleVersionEntity> findByRuleIdOrderByVersionDesc(Long ruleId);
    Optional<DecisionRuleVersionEntity> findByRuleIdAndVersion(Long ruleId, Integer version);
}
