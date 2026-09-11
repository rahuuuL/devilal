package com.terminal_devilal.decision.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public final class DecisionResponses {
    private DecisionResponses() {}
    public record ProfileResponse(UUID id,Long ownerId,String code,String name,String description,String evaluationMode,String subjectType,Boolean enabled) {}
    public record IndicatorResponse(String code,String name,String category,String subjectType,String valueType,String unit,String sourceType,String sourceReference,String sourceProviderCode,String rowFilterExpression,String fieldExpression,String rowAggregation,Boolean enabled,Object minValue,Object maxValue,String description,LocalDateTime createdAt,LocalDateTime updatedAt) {}
    public record IndicatorParameterResponse(Long id,String indicatorCode,String parameterCode,String parameterName,String valueType,Boolean required,String defaultValueJson,String resolutionType,String dynamicExpression,Object minValue,Object maxValue,String description,Integer sequenceNo,Boolean enabled,LocalDateTime createdAt,LocalDateTime updatedAt) {}
    public record OutputResponse(Long id,String profileCode,String code,String name,String valueType,String initialValue,Object minValue,Object maxValue) {}
    public record RuleResponse(UUID id,Long ownerId,String profileCode,String code,String name,String ruleType,Integer priority,String status,Boolean enabled,Integer version) {}
    public record VersionResponse(Integer version,String status,String definition) {}
    public record RuleEvaluationResponse(String rule,String status) {}
    public record SubjectResult(String subjectType,String subjectId,LocalDate asOfDate,Map<String,Object> inputAttributes,Map<String,Object> indicatorValues,Map<String,Object> outputs,List<String> rulesFired) {}
    public record EvaluationResponse(String profileCode,LocalDate asOfDate,List<SubjectResult> results) {}
}
