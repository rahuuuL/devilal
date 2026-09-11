package com.terminal_devilal.decision.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "decision_indicator_parameter")
public class DecisionIndicatorParameterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_code", nullable = false, length = 100)
    private String indicatorCode;

    @Column(name = "parameter_code", nullable = false, length = 100)
    private String parameterCode;

    @Column(name = "parameter_name", nullable = false, length = 200)
    private String parameterName;

    @Column(name = "value_type", nullable = false, length = 30)
    private String valueType;

    @Column(nullable = false)
    private Boolean required = true;

    @Column(name = "default_value_json", columnDefinition = "json")
    private String defaultValueJson;

    @Column(name = "resolution_type", nullable = false, length = 30)
    private String resolutionType = "STATIC";

    @Column(name = "dynamic_expression", length = 500)
    private String dynamicExpression;

    @Column(name = "min_value", precision = 30, scale = 12)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 30, scale = 12)
    private BigDecimal maxValue;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected DecisionIndicatorParameterEntity() {
    }

    public DecisionIndicatorParameterEntity(String indicatorCode, String parameterCode, String parameterName, String valueType,
                                          Boolean required, String defaultValueJson, String resolutionType, String dynamicExpression,
                                          BigDecimal minValue, BigDecimal maxValue, String description, Integer sequenceNo,
                                          Boolean enabled) {
        this.indicatorCode = indicatorCode;
        this.parameterCode = parameterCode;
        this.parameterName = parameterName;
        this.valueType = valueType;
        this.required = required == null ? true : required;
        this.defaultValueJson = defaultValueJson;
        this.resolutionType = resolutionType == null ? "STATIC" : resolutionType;
        this.dynamicExpression = dynamicExpression;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.description = description;
        this.sequenceNo = sequenceNo == null ? 0 : sequenceNo;
        this.enabled = enabled == null ? true : enabled;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public String getIndicatorCode() { return indicatorCode; }
    public String getParameterCode() { return parameterCode; }
    public String getParameterName() { return parameterName; }
    public String getValueType() { return valueType; }
    public Boolean getRequired() { return required; }
    public String getDefaultValueJson() { return defaultValueJson; }
    public String getResolutionType() { return resolutionType; }
    public String getDynamicExpression() { return dynamicExpression; }
    public BigDecimal getMinValue() { return minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public String getDescription() { return description; }
    public Integer getSequenceNo() { return sequenceNo; }
    public Boolean getEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; this.updatedAt = LocalDateTime.now(); }
    public void setParameterCode(String parameterCode) { this.parameterCode = parameterCode; this.updatedAt = LocalDateTime.now(); }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; this.updatedAt = LocalDateTime.now(); }
    public void setValueType(String valueType) { this.valueType = valueType; this.updatedAt = LocalDateTime.now(); }
    public void setRequired(Boolean required) { this.required = required == null ? true : required; this.updatedAt = LocalDateTime.now(); }
    public void setDefaultValueJson(String defaultValueJson) { this.defaultValueJson = defaultValueJson; this.updatedAt = LocalDateTime.now(); }
    public void setResolutionType(String resolutionType) { this.resolutionType = resolutionType == null ? "STATIC" : resolutionType; this.updatedAt = LocalDateTime.now(); }
    public void setDynamicExpression(String dynamicExpression) { this.dynamicExpression = dynamicExpression; this.updatedAt = LocalDateTime.now(); }
    public void setMinValue(BigDecimal minValue) { this.minValue = minValue; this.updatedAt = LocalDateTime.now(); }
    public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; this.updatedAt = LocalDateTime.now(); }
    public void setDescription(String description) { this.description = description; this.updatedAt = LocalDateTime.now(); }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo == null ? 0 : sequenceNo; this.updatedAt = LocalDateTime.now(); }
    public void setEnabled(Boolean enabled) { this.enabled = enabled == null ? true : enabled; this.updatedAt = LocalDateTime.now(); }

    public void update(String indicatorCode, String parameterCode, String parameterName, String valueType, Boolean required,
                      String defaultValueJson, String resolutionType, String dynamicExpression, BigDecimal minValue,
                      BigDecimal maxValue, String description, Integer sequenceNo, Boolean enabled) {
        this.indicatorCode = indicatorCode;
        this.parameterCode = parameterCode;
        this.parameterName = parameterName;
        this.valueType = valueType;
        this.required = required == null ? true : required;
        this.defaultValueJson = defaultValueJson;
        this.resolutionType = resolutionType == null ? "STATIC" : resolutionType;
        this.dynamicExpression = dynamicExpression;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.description = description;
        this.sequenceNo = sequenceNo == null ? 0 : sequenceNo;
        this.enabled = enabled == null ? true : enabled;
        this.updatedAt = LocalDateTime.now();
    }
}
