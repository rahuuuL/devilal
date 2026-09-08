package com.terminal_devilal.decision.repository;

import com.terminal_devilal.decision.entity.DecisionProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DecisionProfileRepository extends JpaRepository<DecisionProfileEntity, Long> {
    Optional<DecisionProfileEntity> findByPublicId(UUID publicId);
    Optional<DecisionProfileEntity> findByOwnerIdAndCode(Long ownerId, String code);
    List<DecisionProfileEntity> findByOwnerIdOrderByCode(Long ownerId);
}
