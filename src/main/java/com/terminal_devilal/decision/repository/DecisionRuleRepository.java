package com.terminal_devilal.decision.repository;

import com.terminal_devilal.decision.entity.DecisionRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DecisionRuleRepository extends JpaRepository<DecisionRuleEntity, Long> {
    Optional<DecisionRuleEntity> findByPublicId(UUID publicId);
    Optional<DecisionRuleEntity> findByOwnerIdAndCode(Long ownerId, String code);
    List<DecisionRuleEntity> findByOwnerIdOrderByPriorityDescCodeAsc(Long ownerId);
    List<DecisionRuleEntity> findByProfileIdAndStatusAndEnabledTrueOrderByPriorityDesc(Long profileId, String status);
}
