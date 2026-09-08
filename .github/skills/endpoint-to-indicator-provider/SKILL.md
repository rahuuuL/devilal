---
name: endpoint-to-indicator-provider
description: "Use when turning an existing REST endpoint, service method, or response object into decision-engine indicator providers. Trigger phrases include make this endpoint a provider, create providers from this API, expose these result fields as indicators, provider inputs and outputs, and register an endpoint-backed rule indicator."
---

# Endpoint-to-Indicator Provider

Use this workflow when the user gives an existing endpoint or service and wants its result fields available to the decision/rule engine.

## User Input Contract

Extract these details from the request before editing:

- Source endpoint or service method
- Input fields and their types
- Subject identity field, usually ticker, account, order, or portfolio ID
- Date fields and whether the result is for an as-of date or a date range
- Optional parameters such as `days`, lookback, window, thresholds, or configuration IDs
- Result fields to expose as indicators
- Indicator codes and value types
- Rule comparison examples, if provided

If one of these is missing, inspect the endpoint, DTO, service, repository, and nearby tests before asking a question. Prefer the existing service/repository path over calling a controller from a provider.

## Required Architecture

Implement the provider flow in this order:

```text
rule condition
  -> indicator code
  -> IndicatorProviderRegistry
  -> typed IndicatorEvaluationContext
  -> provider
  -> existing domain service
  -> result field
```

Do not make the provider call an HTTP endpoint inside the same application. Reuse the endpoint's service or repository abstraction.

## Context Design

Keep `IndicatorEvaluationContext` as the parent context containing only shared identity:

```java
public class IndicatorEvaluationContext {
    private final String subjectType;
    private final String subjectId;
}
```

Create a typed child context for each domain when it has domain-specific inputs. For example:

```java
public class DomainIndicatorEvaluationContext extends IndicatorEvaluationContext {
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer days;
}
```

Use a separate child context for volume, MK, RSI, or another domain instead of adding unrelated fields to the parent.

The execution service must instantiate the appropriate child context based on the indicator family. Providers must verify the expected context type and return `null` with a useful warning when it is missing.

## Provider Rules

Create one scalar provider per independently comparable result field.

For an object such as:

```text
score, tau, slope, trend
```

prefer:

```text
MK_SCORE
MK_TAU
MK_SLOPE
MK_TREND
```

Use a composite provider only when the result is inherently one boolean or one inseparable value. A generic numeric rule evaluator should not be expected to evaluate object paths such as `result.score`.

Providers should:

- declare a stable `getIndicatorCode()`
- accept the shared provider contract
- read typed context and resolved parameters
- call the existing domain service
- return one scalar field
- log subject, effective dates, parameters, source result, and returned value
- avoid accepting precomputed indicator values from the request attributes

## Parameter Resolution

Resolve parameters in this order:

1. typed context values
2. rule condition `parameters`
3. domain defaults

Convert serialized values explicitly:

- ISO date strings to `LocalDate`
- numeric JSON values to `Integer`, `Long`, or `Double` as required
- blank ticker lists to an empty list

An empty or absent ticker list should preserve the endpoint's existing all-subject behavior when that is the source contract.

## Service and Repository Reuse

If the endpoint already has a service method for the required query, make that method public and use it from the provider. Keep repository access inside the domain service.

The service method should support:

- date range filtering
- optional subject/ticker filtering
- optional domain parameters
- deterministic ordering so the provider can select the correct current record

Do not duplicate query logic in every provider.

## Indicator Registration

For each provider, register an enabled decision indicator with:

- code
- name
- category
- subject type
- value type
- source type `PROVIDER`
- provider source reference
- description

If Liquibase is used, add a new migration and changelog entry. Never modify an already-applied migration. Use an id after the current latest migration.

## Rule Behavior

Conditions inside one rule are ANDed by the evaluator. Use one rule when several provider values must all pass:

```json
{
  "conditions": [
    {"indicator": "MK_SCORE", "operator": "GREATER_THAN_OR_EQUAL", "value": 5},
    {"indicator": "MK_TAU", "operator": "GREATER_THAN", "value": 0.3},
    {"indicator": "MK_SLOPE", "operator": "GREATER_THAN", "value": 0}
  ],
  "actions": [{"target": "MATCHED", "type": "SELECT"}]
}
```

Separate rules are independent unless the engine explicitly supports stages or dependencies. Do not describe separate rules as a funnel or AND combination without verifying the evaluator semantics.

## Response and Observability

Expose resolved provider values in the evaluation response when the response model supports indicator values. At minimum, logs must show:

- provider and indicator code
- subject ID
- effective date range
- resolved parameters
- source result value
- condition result
- fired rules
- final outputs

## Tests

Add focused tests for:

- provider returns the requested result field
- context date and optional parameters reach the service
- ticker/subject filtering is preserved
- missing result returns `null` without crashing
- multiple conditions in one rule use AND behavior
- default parameters are used only when custom values are absent

Run the narrow provider tests first, then compile or run the decision-engine test suite.

## Completion Checklist

Before finishing, verify:

- [ ] Parent and child contexts compile and are used correctly
- [ ] Provider codes are unique and registered
- [ ] Existing service/repository behavior is reused
- [ ] New Liquibase changes are append-only
- [ ] Rule validation recognizes every indicator
- [ ] Inputs and result fields are represented in logs/response
- [ ] Existing callers still compile
- [ ] Focused tests or compilation pass
