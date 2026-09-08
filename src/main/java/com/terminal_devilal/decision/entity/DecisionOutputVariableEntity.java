package com.terminal_devilal.decision.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="decision_output_variable", uniqueConstraints=@UniqueConstraint(name="uk_output_variable_profile_code", columnNames={"profile_id","code"}))
public class DecisionOutputVariableEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="profile_id", nullable=false) private DecisionProfileEntity profile;
    @Column(nullable=false, length=100) private String code;
    @Column(nullable=false, length=200) private String name;
    @Column(name="value_type", nullable=false, length=30) private String valueType;
    @Column(name="initial_value", length=200) private String initialValue;
    @Column(name="min_value", precision=20, scale=8) private BigDecimal minValue;
    @Column(name="max_value", precision=20, scale=8) private BigDecimal maxValue;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
    protected DecisionOutputVariableEntity(){}
    public DecisionOutputVariableEntity(DecisionProfileEntity profile,String code,String name,String valueType,String initialValue,BigDecimal minValue,BigDecimal maxValue){this.profile=profile;this.code=code;this.name=name;this.valueType=valueType;this.initialValue=initialValue;this.minValue=minValue;this.maxValue=maxValue;}
    public Long getId(){return id;} public DecisionProfileEntity getProfile(){return profile;} public String getCode(){return code;} public String getName(){return name;} public String getValueType(){return valueType;} public String getInitialValue(){return initialValue;} public BigDecimal getMinValue(){return minValue;} public BigDecimal getMaxValue(){return maxValue;}
}
