package com.terminal_devilal.decision.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="decision_rule", uniqueConstraints=@UniqueConstraint(name="uk_decision_rule_owner_code", columnNames={"owner_id","code"}))
public class DecisionRuleEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="public_id", nullable=false, unique=true, columnDefinition="binary(16)") private UUID publicId=UUID.randomUUID();
    @Column(name="owner_id",nullable=false) private Long ownerId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="profile_id",nullable=false) private DecisionProfileEntity profile;
    @Column(nullable=false,length=100) private String code;
    @Column(nullable=false,length=200) private String name;
    @Column(name="rule_type",nullable=false,length=50) private String ruleType;
    @Column(nullable=false) private Integer priority=0;
    @Column(nullable=false,length=30) private String status="DRAFT";
    @Column(nullable=false) private Boolean enabled=true;
    @Column(name="current_version",nullable=false) private Integer currentVersion=1;
    @Column(name="definition_json",nullable=false,columnDefinition="json") private String definitionJson;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    protected DecisionRuleEntity(){}
    public DecisionRuleEntity(Long ownerId,DecisionProfileEntity profile,String code,String name,String ruleType,Integer priority,String definitionJson){this.ownerId=ownerId;this.profile=profile;this.code=code;this.name=name;this.ruleType=ruleType;this.priority=priority==null?0:priority;this.definitionJson=definitionJson;this.createdAt=LocalDateTime.now();this.updatedAt=this.createdAt;}
    public Long getId(){return id;} public UUID getPublicId(){return publicId;} public Long getOwnerId(){return ownerId;} public DecisionProfileEntity getProfile(){return profile;} public String getCode(){return code;} public String getName(){return name;} public String getRuleType(){return ruleType;} public Integer getPriority(){return priority;} public String getStatus(){return status;} public Boolean getEnabled(){return enabled;} public Integer getCurrentVersion(){return currentVersion;} public String getDefinitionJson(){return definitionJson;}
    public void update(String name,String ruleType,Integer priority,String definitionJson){if("ACTIVE".equals(status))throw new IllegalStateException("Active rules must be versioned; deactivate before changing");this.name=name;this.ruleType=ruleType;this.priority=priority==null?0:priority;this.definitionJson=definitionJson;this.currentVersion++;this.status="DRAFT";this.updatedAt=LocalDateTime.now();}
    public void setStatus(String status){this.status=status;this.updatedAt=LocalDateTime.now();} public void setEnabled(boolean enabled){this.enabled=enabled;this.updatedAt=LocalDateTime.now();}
}
