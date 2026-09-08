# Implement Parameterized Indicator Providers in the Decision Rule Engine

We need to modify the existing Dynamic Custom Rule Engine so that indicators can have their own configurable input parameters.

The key requirement is:

> The Decision Engine receives only `subjectType`, `subjectId`, and `asOfDate`.
> If an indicator/provider requires additional parameters, those parameters must be defined/stored in the database as part of the indicator/rule configuration and resolved automatically at runtime.

Do NOT make the caller send indicator values or provider-specific parameters in the evaluation request.

The final execution flow must be:

```text
Decision Request
    |
    | subjectType
    | subjectId
    | asOfDate
    v
Load Active Rules
    |
    v
Resolve Indicator Parameters from DB
    |
    +------------------------------+
    |                              |
    | STATIC                       | DYNAMIC
    | from DB                      | calculated from runtime context
    |                              |
    | baselineWindow=20            | fromDate = asOfDate - 18 months
    | lowPercentile=20             | toDate   = asOfDate
    | highPercentile=80            |
    | ...                          |
    +---------------+--------------+
                    |
                    v
          IndicatorProvider
                    |
                    v
        Existing application method
                    |
                    v
             Indicator Value
                    |
                    v
          Rule condition evaluation
                    |
                    v
             Rule actions
                    |
                    v
             Decision Output
```

The engine remains generic. It must not contain special-case code such as:

```java
if (indicatorCode.equals("CONSISTENT_VOLUME_SCORE")) {
    ...
}
```

---

# 1. Current Provider Contract

The current provider abstraction is approximately:

```java
public interface IndicatorProvider {

    String getIndicatorCode();

    Object getValue(
        String subjectType,
        String subjectId,
        LocalDate asOfDate
    );
}
```

Do NOT make the provider-specific method signature grow like:

```java
getValue(
    subjectType,
    subjectId,
    asOfDate,
    baselineWindow,
    baselineLowPercentile,
    baselineHighPercentile,
    ...
);
```

That would make the generic engine impossible to extend.

Instead introduce a generic resolved parameter object.

Recommended design:

```java
public interface IndicatorProvider {

    String getIndicatorCode();

    Object getValue(
        IndicatorEvaluationContext context,
        Map<String, Object> parameters
    );
}
```

Where:

```java
public class IndicatorEvaluationContext {

    private String subjectType;
    private String subjectId;
    private LocalDate asOfDate;

    // Optional future fields
    // execution date/time
    // profile
    // owner
    // market/session information
}
```

The engine knows the generic context.

The provider knows its own parameters.

---

# 2. Important Parameter Ownership Decision

There are TWO different concepts and they must not be mixed.

## Indicator Parameter Definition

This describes what parameters an indicator supports.

Example:

```text
CONSISTENT_VOLUME_SCORE

baselineWindow
baselineLowPercentile
baselineHighPercentile
rvolPercentileWindow
rvolThresholdPercentile
consistencyWindow
requiredScore
fromDate
toDate
```

This is metadata.

## Rule Parameter Values

This describes what values a particular rule wants to use.

Example:

```text
Rule A:
CONSISTENT_VOLUME_SCORE

baselineWindow = 20
baselineLowPercentile = 20
baselineHighPercentile = 80
rvolPercentileWindow = 60
rvolThresholdPercentile = 75
consistencyWindow = 10
requiredScore = 7
fromDate = DYNAMIC: AS_OF_DATE - 18 MONTHS
toDate = DYNAMIC: AS_OF_DATE
```

Another rule could use:

```text
baselineWindow = 30
baselineLowPercentile = 10
baselineHighPercentile = 90
...
```

Therefore:

> Parameter definitions belong to the indicator registry, while actual parameter values belong to the rule/condition using that indicator.

Do NOT put one global set of parameter values directly on `decision_indicator`, because the same indicator may be used by multiple rules with different configurations.

---

# 3. Database Changes

Current tables include:

```text
decision_profile
decision_indicator
decision_rule
decision_output_variable
decision_rule_version
decision_rule_condition
decision_rule_action
```

Keep these tables.

Add:

```text
decision_indicator_parameter
```

This table defines the parameter schema supported by an indicator.

Recommended schema:

```sql
CREATE TABLE decision_indicator_parameter (
    id BIGINT NOT NULL AUTO_INCREMENT,

    indicator_code VARCHAR(100) NOT NULL,

    parameter_code VARCHAR(100) NOT NULL,

    parameter_name VARCHAR(200) NOT NULL,

    value_type VARCHAR(30) NOT NULL,

    required BOOLEAN NOT NULL DEFAULT TRUE,

    default_value_json JSON,

    resolution_type VARCHAR(30) NOT NULL DEFAULT 'STATIC',

    dynamic_expression VARCHAR(500),

    min_value DECIMAL(30,12),

    max_value DECIMAL(30,12),

    description TEXT,

    sequence_no INT NOT NULL DEFAULT 0,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL,

    updated_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    UNIQUE KEY uk_indicator_parameter (
        indicator_code,
        parameter_code
    ),

    KEY idx_indicator_parameter_indicator (
        indicator_code
    ),

    CONSTRAINT fk_indicator_parameter_indicator
        FOREIGN KEY (indicator_code)
        REFERENCES decision_indicator(code)
);
```

---

# 4. Parameter Resolution Types

Initially support:

```text
STATIC
DYNAMIC
```

Potential future types:

```text
INDICATOR
EXPRESSION
PROFILE
SYSTEM
```

But do not implement unnecessary types initially.

---

# 5. STATIC Parameters

A STATIC parameter comes directly from the rule configuration.

Example:

```text
baselineWindow = 20
baselineLowPercentile = 20
baselineHighPercentile = 80
rvolPercentileWindow = 60
rvolThresholdPercentile = 75
consistencyWindow = 10
requiredScore = 7
```

These values must be stored in the database.

---

# 6. DYNAMIC Parameters

A DYNAMIC parameter is calculated from the runtime evaluation context.

For the current requirement:

```text
fromDate = asOfDate - 18 months
toDate   = asOfDate
```

Therefore if:

```text
asOfDate = 2026-09-08
```

then:

```text
fromDate = 2025-03-08
toDate   = 2026-09-08
```

Use Java date arithmetic:

```java
asOfDate.minusMonths(18)
```

Do NOT calculate this in the UI.

Do NOT require the client to send `fromDate`.

The runtime engine calculates it.

---

# 7. Example Parameter Registry

For:

```text
CONSISTENT_VOLUME_SCORE
```

create these parameter definitions:

```text
baselineWindow
    NUMBER
    STATIC
    required=true

baselineLowPercentile
    NUMBER
    STATIC
    required=true

baselineHighPercentile
    NUMBER
    STATIC
    required=true

rvolPercentileWindow
    NUMBER
    STATIC
    required=true

rvolThresholdPercentile
    NUMBER
    STATIC
    required=true

consistencyWindow
    NUMBER
    STATIC
    required=true

requiredScore
    NUMBER
    STATIC
    required=true

fromDate
    DATE
    DYNAMIC
    expression=AS_OF_DATE_MINUS_MONTHS:18

toDate
    DATE
    DYNAMIC
    expression=AS_OF_DATE
```

---

# 8. Store Actual Rule Values

The existing:

```text
decision_rule_condition
```

already represents the condition using an indicator.

Extend it with:

```sql
ALTER TABLE decision_rule_condition
ADD COLUMN indicator_params_json JSON NULL;
```

This is the actual configuration for the indicator for that rule.

Example:

```json
{
  "baselineWindow": 20,
  "baselineLowPercentile": 20,
  "baselineHighPercentile": 80,
  "rvolPercentileWindow": 60,
  "rvolThresholdPercentile": 75,
  "consistencyWindow": 10,
  "requiredScore": 7
}
```

Do NOT store `fromDate` and `toDate` as fixed dates.

Instead the indicator parameter definition specifies that they are dynamic.

---

# 9. Why Parameters Belong on the Rule Condition

Example:

```text
Rule A
PVPP > 0.002

Rule B
CONSISTENT_VOLUME_SCORE >= 7
```

The condition points to:

```text
indicator_code
```

The indicator has parameter definitions.

The rule condition contains the selected values.

This means:

```text
Indicator
    |
    +-- defines supported parameters
    |
    +-- Provider implementation

Rule Condition
    |
    +-- selects actual parameter values
```

This is the correct separation of concerns.

---

# 10. Parameter Resolution Service

Create:

```java
public interface IndicatorParameterResolver {

    Map<String, Object> resolve(
        String indicatorCode,
        DecisionRuleCondition condition,
        IndicatorEvaluationContext context
    );
}
```

Implementation:

```java
@Component
public class DefaultIndicatorParameterResolver
        implements IndicatorParameterResolver {
}
```

Responsibilities:

```text
1. Load parameter definitions
2. Read configured static values
3. Resolve dynamic values
4. Validate types
5. Validate ranges
6. Return final parameter map
```

---

# 11. Static Parameter Resolution

For:

```json
{
  "baselineWindow": 20,
  "requiredScore": 7
}
```

return:

```java
Map<String, Object>
```

containing:

```text
baselineWindow -> 20
requiredScore -> 7
```

Convert values to the parameter's declared type.

For example:

```text
NUMBER → Integer / BigDecimal
DATE   → LocalDate
BOOLEAN → Boolean
STRING → String
```

Use a consistent numeric representation across the engine.

---

# 12. Dynamic Parameter Resolution

Implement a controlled resolver.

For example:

```text
AS_OF_DATE
AS_OF_DATE_MINUS_MONTHS:18
AS_OF_DATE_MINUS_DAYS:365
AS_OF_DATE_PLUS_DAYS:5
```

For this requirement:

```text
fromDate:
AS_OF_DATE_MINUS_MONTHS:18

toDate:
AS_OF_DATE
```

Example:

```java
private Object resolveDynamic(
        String expression,
        IndicatorEvaluationContext context) {

    if ("AS_OF_DATE".equals(expression)) {
        return context.getAsOfDate();
    }

    if (expression.startsWith("AS_OF_DATE_MINUS_MONTHS:")) {
        int months = ...;
        return context.getAsOfDate().minusMonths(months);
    }

    throw new InvalidParameterExpressionException(expression);
}
```

Do NOT execute arbitrary Java expressions.

Do NOT execute arbitrary SpEL/MVEL supplied directly by users.

Use a controlled whitelist of dynamic operations.

---

# 13. Provider Execution

The provider receives:

```text
IndicatorEvaluationContext
+
resolved parameters
```

Example:

```java
@Component
public class ConsistentVolumeScoreProvider
        implements IndicatorProvider {

    private final ConsistentVolumeDetector detector;

    @Override
    public String getIndicatorCode() {
        return "CONSISTENT_VOLUME_SCORE";
    }

    @Override
    public Object getValue(
            IndicatorEvaluationContext context,
            Map<String, Object> parameters) {

        LocalDate fromDate =
            (LocalDate) parameters.get("fromDate");

        LocalDate toDate =
            (LocalDate) parameters.get("toDate");

        int baselineWindow =
            ((Number) parameters.get("baselineWindow")).intValue();

        double baselineLowPercentile =
            ((Number) parameters.get(
                "baselineLowPercentile")).doubleValue();

        double baselineHighPercentile =
            ((Number) parameters.get(
                "baselineHighPercentile")).doubleValue();

        int baseRvolPercentileWindow =
            ((Number) parameters.get(
                "rvolPercentileWindow")).intValue();

        double rvolThresholdPercentile =
            ((Number) parameters.get(
                "rvolThresholdPercentile")).doubleValue();

        int consistencyWindow =
            ((Number) parameters.get(
                "consistencyWindow")).intValue();

        int requiredScore =
            ((Number) parameters.get(
                "requiredScore")).intValue();

        return detector.computeScoreForTicker(
            context.getSubjectId(),
            fromDate,
            toDate,
            baselineWindow,
            baselineLowPercentile,
            baselineHighPercentile,
            baseRvolPercentileWindow,
            rvolThresholdPercentile,
            consistencyWindow,
            requiredScore
        );
    }
}
```

Adapt this to the actual existing `ConsistentVolumeDetector` API.

---

# 14. Important Existing Method

There is an existing method:

```java
public List<ConsistentVolumeSignalResponse> detectConsistentVolumes(
    LocalDate fromDate,
    LocalDate toDate,
    int inputBaselineWindow,
    double baselineLowPercentile,
    double baselineHighPercentile,
    int baseRvolPercentileWindow,
    double rvolThresholdPercentile,
    int consistencyWindow,
    int requiredScore
)
```

This method is batch/range oriented.

The decision engine, however, needs:

```text
subjectId
asOfDate
```

and one indicator value.

Therefore do not make the generic rule engine understand the batch method.

Instead introduce an adapter/wrapper at the provider level.

For example:

```java
public BigDecimal computeScoreForTicker(
        String ticker,
        LocalDate fromDate,
        LocalDate toDate,
        ...
)
```

This method should reuse the existing detector logic where possible.

Do not duplicate the actual consistent-volume algorithm unnecessarily.

---

# 15. Batch Detector Adapter

If the existing method returns:

```java
List<ConsistentVolumeSignalResponse>
```

the provider adapter can call it and extract the result for the requested subject/date.

Example concept:

```java
List<ConsistentVolumeSignalResponse> results =
    detector.detectConsistentVolumes(
        fromDate,
        toDate,
        ...
    );

return results.stream()
    .filter(x -> ticker.equals(x.getTicker()))
    .filter(x -> asOfDate.equals(x.getDate()))
    .map(ConsistentVolumeSignalResponse::getScore)
    .findFirst()
    .orElse(null);
```

However, if this causes the entire universe to be recalculated for every ticker, DO NOT use this approach in production.

Instead refactor/extract the underlying calculation so that:

```text
batch method
```

and:

```text
single ticker provider method
```

share the same calculation service.

Preferred structure:

```text
ConsistentVolumeCalculationService
          |
          +---- detectConsistentVolumes(...)
          |
          +---- computeScoreForTicker(...)
```

Both reuse the same calculation logic.

---

# 16. Provider Registry

Keep:

```java
IndicatorProviderRegistry
```

generic.

It should only map:

```text
indicatorCode → provider
```

Example:

```text
RVOL → RvolProvider
CONSISTENT_VOLUME_SCORE → ConsistentVolumeScoreProvider
RSI → RsiProvider
ATR → AtrProvider
```

Do not add parameter-specific logic to the registry.

---

# 17. Runtime Engine Flow

Implement exactly this sequence:

```text
1. Receive:
       subjectType
       subjectId
       asOfDate
       profile

2. Load active rule versions.

3. Identify indicators required by those rules.

4. For each required indicator:

       provider = providerRegistry.get(indicatorCode)

5. For each rule condition using that indicator:

       parameter definitions = indicator registry
       parameter values      = rule condition
       runtime context       = subject/date

6. Resolve parameters.

7. Call provider.

8. Put returned value into SubjectContext.

9. Evaluate rule condition.

10. If condition matches:
        execute action

11. Continue through all rules according to profile evaluation mode.

12. Return DecisionResult.
```

---

# 18. Example End-to-End Execution

Request:

```json
{
  "profileCode": "MOMENTUM",
  "subjectType": "TICKER",
  "subjectId": "RELIANCE",
  "asOfDate": "2026-09-08"
}
```

The client sends NOTHING else.

The rule contains:

```text
Indicator:
CONSISTENT_VOLUME_SCORE
```

Rule configuration:

```json
{
  "baselineWindow": 20,
  "baselineLowPercentile": 20,
  "baselineHighPercentile": 80,
  "rvolPercentileWindow": 60,
  "rvolThresholdPercentile": 75,
  "consistencyWindow": 10,
  "requiredScore": 7
}
```

Indicator parameter metadata says:

```text
fromDate = AS_OF_DATE_MINUS_MONTHS:18
toDate   = AS_OF_DATE
```

Runtime resolves:

```text
subjectId = RELIANCE

asOfDate = 2026-09-08

fromDate = 2025-03-08
toDate   = 2026-09-08

baselineWindow = 20
baselineLowPercentile = 20
baselineHighPercentile = 80
rvolPercentileWindow = 60
rvolThresholdPercentile = 75
consistencyWindow = 10
requiredScore = 7
```

Provider executes:

```java
computeScoreForTicker(
    "RELIANCE",
    2025-03-08,
    2026-09-08,
    20,
    20,
    80,
    60,
    75,
    10,
    7
);
```

Suppose it returns:

```text
8
```

The rule engine now sees:

```text
CONSISTENT_VOLUME_SCORE = 8
```

and evaluates:

```text
8 >= 7
```

Result:

```text
TRUE
```

The configured rule action executes.

The final output is returned.

---
# 21. UI Changes

The UI must obtain parameter definitions from the backend.

Add:

```http
GET /api/decision/indicators/{indicatorCode}/parameters
```

Example response:

```json
[
  {
    "code": "baselineWindow",
    "name": "Baseline Window",
    "valueType": "NUMBER",
    "resolutionType": "STATIC",
    "required": true
  },
  {
    "code": "baselineLowPercentile",
    "name": "Baseline Low Percentile",
    "valueType": "NUMBER",
    "resolutionType": "STATIC",
    "required": true
  },
  {
    "code": "fromDate",
    "name": "From Date",
    "valueType": "DATE",
    "resolutionType": "DYNAMIC",
    "dynamicExpression": "AS_OF_DATE_MINUS_MONTHS:18",
    "required": true
  },
  {
    "code": "toDate",
    "name": "To Date",
    "valueType": "DATE",
    "resolutionType": "DYNAMIC",
    "dynamicExpression": "AS_OF_DATE",
    "required": true
  }
]
```

The UI should show static parameters as configurable fields.

Dynamic parameters should be displayed as:

```text
From Date
[ Dynamic: 18 months before As-Of Date ]

To Date
[ Dynamic: As-Of Date ]
```

The user should NOT enter actual dates.

---

# 22. Rule JSON

The rule definition should eventually look like:

```json
{
  "conditions": [
    {
      "indicator": "CONSISTENT_VOLUME_SCORE",
      "operator": ">=",
      "comparisonType": "CONSTANT",
      "comparisonValue": 7,
      "indicatorParams": {
        "baselineWindow": 20,
        "baselineLowPercentile": 20,
        "baselineHighPercentile": 80,
        "rvolPercentileWindow": 60,
        "rvolThresholdPercentile": 75,
        "consistencyWindow": 10,
        "requiredScore": 7
      }
    }
  ]
}
```

Do not put:

```text
fromDate
toDate
```

inside this JSON as fixed dates.

Those are derived from the runtime context.

---

# 23. Validation

When creating/activating a rule, validate:

```text
✓ indicator exists
✓ indicator enabled
✓ provider exists
✓ parameter definition exists
✓ all required parameters supplied
✓ no unknown parameters
✓ parameter data types are correct
✓ static values are within min/max
✓ dynamic expressions are supported
✓ dynamic expression produces expected type
✓ no arbitrary expression/code
```

Example:

```text
requiredScore = "hello"
```

must fail validation.

Also:

```text
unknownParameter = 123
```

must fail validation.

---

# 24. Provider Contract Compatibility

If changing the existing interface from:

```java
Object getValue(
    String subjectType,
    String subjectId,
    LocalDate asOfDate
);
```

to:

```java
Object getValue(
    IndicatorEvaluationContext context,
    Map<String, Object> parameters
);
```

would create unnecessary impact, introduce an adapter/default method instead.

Inspect all existing usages before changing the interface.

The implementation must preserve backward compatibility where practical.

---

# 25. Caching

Parameter definitions are metadata and can be cached.

Active rule parameter values can also be included in the compiled rule/profile cache.

However:

```text
asOfDate
```

must always be evaluated per execution.

Do NOT cache:

```text
fromDate
toDate
```

as permanent values.

For:

```text
asOfDate = 2026-09-08
```

resolve:

```text
fromDate = 2025-03-08
```

For another execution:

```text
asOfDate = 2026-10-08
```

resolve:

```text
fromDate = 2025-04-08
```

---

# 26. Important Performance Requirement

Do not execute expensive provider calculations once for every condition if multiple rules use the same indicator with identical parameters.

Introduce an indicator evaluation cache for the current decision execution.

Conceptually:

```text
Indicator Cache Key:

indicatorCode
+
subjectType
+
subjectId
+
asOfDate
+
resolvedParametersHash
```

Example:

```text
CONSISTENT_VOLUME_SCORE
RELIANCE
2026-09-08
hash(parameters)
```

If two rules use the exact same indicator and parameter configuration, calculate once.

If parameters differ, calculate separately.

---

# 27. Batch Optimization

For screeners evaluating thousands of tickers, the provider should support batch optimization in the future.

The abstraction should allow:

```java
resolveValues(
    Set<String> subjectIds,
    IndicatorEvaluationContext context,
    Map<String, Object> parameters
)
```

but do not make batch execution mandatory for the first implementation.

The important requirement is that the provider architecture does not prevent an optimized batch implementation later.

---

# 28. Do Not Do This

Do NOT implement:

```java
@Component
public class ConsistentVolumeScoreProvider {

    @Value("${volume.baselineWindow}")
    private int baselineWindow;
}
```

This is incorrect for the decision engine because rule configuration belongs in the database.

Do NOT require:

```json
{
  "subjectId": "RELIANCE",
  "asOfDate": "2026-09-08",
  "baselineWindow": 20,
  "requiredScore": 7
}
```

from the caller.

Do NOT make the engine know:

```text
baselineWindow
baselineLowPercentile
consistencyWindow
requiredScore
```

These are provider-specific parameters.

---

# 29. Desired Separation of Responsibilities

## Decision Engine

Knows:

```text
subject
date
rules
indicators
providers
conditions
actions
decision state
```

Does NOT know:

```text
how consistent volume is calculated
what baselineWindow means
what rvolThresholdPercentile means
```

## Indicator Registry

Knows:

```text
indicator exists
indicator type
provider reference
parameter definitions
```

## Rule Condition

Knows:

```text
which indicator
which condition
which parameter values
```

## Parameter Resolver

Knows:

```text
how to convert DB configuration
into runtime parameters
```

## Provider

Knows:

```text
how to calculate its indicator
```

## Existing Detector

Knows:

```text
actual business/calculation algorithm
```

This separation is mandatory.

---

# 30. Required Files to Create/Modify

Inspect the existing project first.

Likely files:

```text
IndicatorProvider.java
IndicatorProviderRegistry.java

DecisionRuleConditionEntity.java
DecisionIndicatorEntity.java

DecisionRuleConditionRepository.java
DecisionIndicatorRepository.java

DecisionEngine.java
DecisionService.java

ConsistentVolumeScoreProvider.java
RvolProvider.java

IndicatorParameterResolver.java
DefaultIndicatorParameterResolver.java

IndicatorEvaluationContext.java

DecisionIndicatorParameterEntity.java
DecisionIndicatorParameterRepository.java
```

Also modify:

```text
decision_rule_condition
```

to support:

```text
indicator_params_json
```

---

# 31. Database Migration

Create a migration using the project's EXISTING migration framework.

Do not introduce a new migration framework.

Migration must:

```text
1. Create decision_indicator_parameter
2. Add indicator_params_json to decision_rule_condition
3. Add indexes/foreign keys
4. Insert parameter definitions for CONSISTENT_VOLUME_SCORE
5. Insert parameter definitions for RVOL if required
```

Do not modify existing production data destructively.

---

# 32. Tests

Add unit tests for:

```text
StaticParameterResolutionTest
DynamicDateParameterResolutionTest
IndicatorParameterValidationTest
ConsistentVolumeScoreProviderTest
IndicatorProviderRegistryTest
```

Specifically test:

```text
asOfDate = 2026-09-08

fromDate = 2025-03-08
toDate = 2026-09-08
```

Test:

```text
baselineWindow = 20
requiredScore = 7
```

Test invalid:

```text
baselineWindow = "ABC"
```

Test unknown:

```text
unknownParameter = 123
```

Test missing required parameter.

Test disabled indicator.

Test provider not found.

---

# 33. End-to-End Acceptance Test

The following must work:

### Database

Indicator:

```text
CONSISTENT_VOLUME_SCORE
```

Parameter definitions:

```text
baselineWindow
baselineLowPercentile
baselineHighPercentile
rvolPercentileWindow
rvolThresholdPercentile
consistencyWindow
requiredScore
fromDate
toDate
```

Rule condition:

```text
CONSISTENT_VOLUME_SCORE >= 7
```

Static configuration:

```text
baselineWindow = 20
baselineLowPercentile = 20
baselineHighPercentile = 80
rvolPercentileWindow = 60
rvolThresholdPercentile = 75
consistencyWindow = 10
requiredScore = 7
```

Dynamic configuration:

```text
fromDate = AS_OF_DATE_MINUS_MONTHS:18
toDate = AS_OF_DATE
```

### Request

```json
{
  "profileCode": "MOMENTUM",
  "subjectType": "TICKER",
  "subjectId": "RELIANCE",
  "asOfDate": "2026-09-08"
}
```

### Runtime

Provider receives:

```text
subjectId = RELIANCE

fromDate = 2025-03-08
toDate = 2026-09-08

baselineWindow = 20
baselineLowPercentile = 20
baselineHighPercentile = 80
rvolPercentileWindow = 60
rvolThresholdPercentile = 75
consistencyWindow = 10
requiredScore = 7
```

Provider calls the existing consistent-volume calculation through an adapter/service.

Suppose result:

```text
CONSISTENT_VOLUME_SCORE = 8
```

Then:

```text
8 >= 7
```

returns:

```text
TRUE
```

The rule action executes and the final decision is returned.

---

# 34. Final Architecture

The resulting architecture must be:

```text
                         DECISION REQUEST
                               |
                               v
                 subjectType / subjectId / asOfDate
                               |
                               v
                       Decision Engine
                               |
                               v
                       Active Rules
                               |
                               v
                     Rule Condition
                               |
                               v
                       Indicator Code
                               |
                               v
                  +-----------------------+
                  | Indicator Registry    |
                  |                       |
                  | Provider              |
                  | Parameter Definitions |
                  +----------+------------+
                             |
                             v
                   Parameter Resolver
                             |
                 +-----------+-----------+
                 |                       |
                 v                       v
             STATIC                  DYNAMIC
             DB values              Runtime context
                 |                       |
                 |                 asOfDate - 18 months
                 |                 asOfDate
                 +-----------+-----------+
                             |
                             v
                    Indicator Provider
                             |
                             v
                  Existing Calculation
                             |
                             v
                    Indicator Value
                             |
                             v
                    Rule Evaluation
                             |
                             v
                       Rule Action
                             |
                             v
                    Decision Output
```

## Core principle

The engine should be completely unaware of provider-specific parameters.

The database describes:

```text
what parameters an indicator supports
```

The rule describes:

```text
what values this rule wants
```

The runtime context supplies:

```text
subjectId
asOfDate
```

The parameter resolver produces:

```text
final provider parameters
```

The provider performs:

```text
actual calculation
```

The engine receives only:

```text
indicator result
```

and continues normal rule evaluation.

---

# 35. Implementation Instructions for Copilot

Before changing code:

1. Inspect the entire existing decision-engine implementation.
2. Inspect the existing `IndicatorProvider.java`.
3. Inspect `IndicatorProviderRegistry.java`.
4. Inspect all existing implementations/usages of `IndicatorProvider`.
5. Inspect `DecisionRuleConditionEntity`.
6. Inspect `DecisionIndicatorEntity`.
7. Inspect the existing rule JSON structure.
8. Inspect the existing database migration framework.
9. Inspect `ConsistentVolumeDetector`.
10. Inspect `ConsistentVolumeSignalResponse`.
11. Inspect the existing RVOL implementation.
12. Inspect the existing decision execution service.

Then implement the parameterized-provider architecture above.

Do not rewrite unrelated functionality.

Do not create duplicate calculation logic if an existing service can be adapted.

Do not introduce hard-coded indicator-specific logic into the generic Decision Engine.

After implementation, provide:

```text
1. Files created
2. Files modified
3. DB migration created
4. Gradle changes, if any
5. API changes
6. Runtime execution flow
7. Tests added
8. Any backward compatibility concerns
9. Any assumptions made
10. Exact example request/response
```

The implementation should compile and all existing tests should continue to pass.
