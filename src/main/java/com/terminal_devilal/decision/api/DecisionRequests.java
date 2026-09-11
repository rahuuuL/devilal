package com.terminal_devilal.decision.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DecisionRequests {
    private DecisionRequests() {}
    public record ProfileRequest(@NotNull Long ownerId,@NotBlank String code,@NotBlank String name,String description,@NotBlank String evaluationMode,@NotBlank String subjectType,Boolean enabled) {}
    public record IndicatorRequest(@NotBlank String code,@NotBlank String name,@NotBlank String category,@NotBlank String subjectType,@NotBlank String valueType,String unit,@NotBlank String sourceType,String sourceReference,String sourceProviderCode,String rowFilterExpression,String fieldExpression,String rowAggregation,BigDecimal minValue,BigDecimal maxValue,String description,Boolean enabled) {}
    public record IndicatorPatchRequest(String name,String category,String subjectType,String valueType,String unit,String sourceType,String sourceReference,String sourceProviderCode,String rowFilterExpression,String fieldExpression,String rowAggregation,BigDecimal minValue,BigDecimal maxValue,String description,Boolean enabled) {}
    public record IndicatorParameterRequest(String indicatorCode,String parameterCode,String parameterName,String valueType,Boolean required,String defaultValueJson,String resolutionType,String dynamicExpression,BigDecimal minValue,BigDecimal maxValue,String description,Integer sequenceNo,Boolean enabled) {}
    public record OutputRequest(@NotNull Long ownerId,@NotBlank String profileCode,@NotBlank String code,@NotBlank String name,@NotBlank String valueType,String initialValue,BigDecimal minValue,BigDecimal maxValue) {}
    public record RuleRequest(@NotNull Long ownerId,@NotBlank String profileCode,@NotBlank String code,@NotBlank String name,@NotBlank String ruleType,Integer priority,@NotNull JsonNode definition) {}
    public record SubjectRequest(@NotBlank String subjectType,@NotBlank String subjectId,LocalDate asOfDate,@NotNull java.util.Map<String,Object> attributes) {}
    public record EvaluationRequest(@NotNull Long ownerId,@NotBlank String profileCode,LocalDate asOfDate,@NotBlank String subjectType,@NotNull List<SubjectRequest> subjects) {}
}
