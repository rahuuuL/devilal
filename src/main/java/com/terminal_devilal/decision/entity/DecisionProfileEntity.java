package com.terminal_devilal.decision.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "decision_profile", uniqueConstraints = @UniqueConstraint(name = "uk_decision_profile_owner_code", columnNames = {"owner_id", "code"}))
public class DecisionProfileEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "binary(16)") private UUID publicId = UUID.randomUUID();
    @Column(name = "owner_id", nullable = false) private Long ownerId;
    @Column(nullable = false, length = 100) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "evaluation_mode", nullable = false, length = 30) private String evaluationMode = "ACCUMULATE";
    @Column(name = "subject_type", nullable = false, length = 50) private String subjectType;
    @Column(nullable = false) private Boolean enabled = true;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected DecisionProfileEntity() {}
    public DecisionProfileEntity(Long ownerId, String code, String name, String description, String evaluationMode, String subjectType) {
        this.ownerId = ownerId; this.code = code; this.name = name; this.description = description;
        this.evaluationMode = evaluationMode; this.subjectType = subjectType;
        this.createdAt = LocalDateTime.now(); this.updatedAt = this.createdAt;
    }
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Long getOwnerId() { return ownerId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getEvaluationMode() { return evaluationMode; }
    public String getSubjectType() { return subjectType; }
    public Boolean getEnabled() { return enabled; }
    public void update(String name, String description, String evaluationMode, String subjectType, Boolean enabled) {
        this.name = name; this.description = description; this.evaluationMode = evaluationMode; this.subjectType = subjectType;
        if (enabled != null) this.enabled = enabled; this.updatedAt = LocalDateTime.now();
    }
}
