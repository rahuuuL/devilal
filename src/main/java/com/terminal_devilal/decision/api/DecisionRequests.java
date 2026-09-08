package com.terminal_devilal.decision.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DecisionRequests {
    private DecisionRequests() {}
    public record ProfileRequest(@NotNull Long ownerId,@NotBlank String code,@NotBlank String name,String description,@NotBlank String evaluationMode,@NotBlank String subjectType,Boolean enabled) {}
    public record IndicatorRequest(@NotBlank String code,@NotBlank String name,@NotBlank String category,@NotBlank String subjectType,@NotBlank String valueType,String unit,@NotBlank String sourceType,String sourceReference,BigDecimal minValue,BigDecimal maxValue,String description,Boolean enabled) {}
    public record OutputRequest(@NotNull Long ownerId,@NotBlank String profileCode,@NotBlank String code,@NotBlank String name,@NotBlank String valueType,String initialValue,BigDecimal minValue,BigDecimal maxValue) {}
    public record RuleRequest(@NotNull Long ownerId,@NotBlank String profileCode,@NotBlank String code,@NotBlank String name,@NotBlank String ruleType,Integer priority,@NotNull JsonNode definition) {}
    public record SubjectRequest(@NotBlank String subjectType,@NotBlank String subjectId,LocalDate asOfDate,@NotNull java.util.Map<String,Object> attributes) {}
    public record EvaluationRequest(@NotNull Long ownerId,@NotBlank String profileCode,LocalDate asOfDate,@NotBlank String subjectType,@NotNull List<SubjectRequest> subjects) {}
}
