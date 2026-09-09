package com.terminal_devilal.decision.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "decision_indicator")
public class DecisionIndicatorEntity {
    @Id @Column(length = 100) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 100) private String category;
    @Column(name = "subject_type", nullable = false, length = 50) private String subjectType;
    @Column(name = "value_type", nullable = false, length = 30) private String valueType;
    @Column(length = 50) private String unit;
    @Column(name = "source_type", nullable = false, length = 30) private String sourceType;
    @Column(name = "source_reference", length = 200) private String sourceReference;
    @Column(name = "source_provider_code", length = 100) private String sourceProviderCode;
    @Column(name = "row_filter_expression", length = 200) private String rowFilterExpression;
    @Column(name = "field_expression", length = 200) private String fieldExpression;
    @Column(name = "row_aggregation", length = 20) private String rowAggregation;
    @Column(nullable = false) private Boolean enabled = true;
    @Column(name = "min_value", precision = 20, scale = 8) private BigDecimal minValue;
    @Column(name = "max_value", precision = 20, scale = 8) private BigDecimal maxValue;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    protected DecisionIndicatorEntity() {}
    public DecisionIndicatorEntity(String code, String name, String category, String subjectType, String valueType, String unit, String sourceType, String sourceReference, BigDecimal minValue, BigDecimal maxValue, String description) {
        this.code=code; this.name=name; this.category=category; this.subjectType=subjectType; this.valueType=valueType; this.unit=unit; this.sourceType=sourceType; this.sourceReference=sourceReference; this.minValue=minValue; this.maxValue=maxValue; this.description=description; this.createdAt=LocalDateTime.now(); this.updatedAt=this.createdAt;
    }
    public String getCode(){return code;} public String getName(){return name;} public String getCategory(){return category;} public String getSubjectType(){return subjectType;} public String getValueType(){return valueType;} public String getUnit(){return unit;} public String getSourceType(){return sourceType;} public String getSourceReference(){return sourceReference;} public String getSourceProviderCode(){return sourceProviderCode;} public String getRowFilterExpression(){return rowFilterExpression;} public String getFieldExpression(){return fieldExpression;} public String getRowAggregation(){return rowAggregation;} public Boolean getEnabled(){return enabled;} public BigDecimal getMinValue(){return minValue;} public BigDecimal getMaxValue(){return maxValue;} public String getDescription(){return description;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setSourceProviderCode(String sourceProviderCode){this.sourceProviderCode=sourceProviderCode;} public void setRowFilterExpression(String rowFilterExpression){this.rowFilterExpression=rowFilterExpression;} public void setFieldExpression(String fieldExpression){this.fieldExpression=fieldExpression;} public void setRowAggregation(String rowAggregation){this.rowAggregation=rowAggregation;}
    public void update(String name, String category, String subjectType, String valueType, String unit, String sourceType, String sourceReference, String sourceProviderCode, String rowFilterExpression, String fieldExpression, String rowAggregation, BigDecimal minValue, BigDecimal maxValue, String description, Boolean enabled) { this.name=name;this.category=category;this.subjectType=subjectType;this.valueType=valueType;this.unit=unit;this.sourceType=sourceType;this.sourceReference=sourceReference;this.sourceProviderCode=sourceProviderCode;this.rowFilterExpression=rowFilterExpression;this.fieldExpression=fieldExpression;this.rowAggregation=rowAggregation;this.minValue=minValue;this.maxValue=maxValue;this.description=description;if(enabled!=null)this.enabled=enabled;this.updatedAt=LocalDateTime.now(); }
    public void setEnabled(Boolean enabled){this.enabled=enabled;this.updatedAt=LocalDateTime.now();}
}
