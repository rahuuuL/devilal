package com.terminal_devilal.decision.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="decision_rule_version",uniqueConstraints=@UniqueConstraint(name="uk_decision_rule_version",columnNames={"rule_id","version"}))
public class DecisionRuleVersionEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="rule_id",nullable=false) private DecisionRuleEntity rule;
    @Column(nullable=false) private Integer version;
    @Column(name="definition_json",nullable=false,columnDefinition="json") private String definitionJson;
    @Column(nullable=false,length=30) private String status;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    protected DecisionRuleVersionEntity(){}
    public DecisionRuleVersionEntity(DecisionRuleEntity rule,Integer version,String definitionJson,String status){this.rule=rule;this.version=version;this.definitionJson=definitionJson;this.status=status;}
    public Long getId(){return id;} public DecisionRuleEntity getRule(){return rule;} public Integer getVersion(){return version;} public String getDefinitionJson(){return definitionJson;} public String getStatus(){return status;}
    public void setStatus(String status){this.status=status;}
}
