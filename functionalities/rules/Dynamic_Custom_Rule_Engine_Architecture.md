# Dynamic Custom Rule Engine — Baseline Architecture
### (Generalized from the original Decision & Risk Rule Engine design — risk scoring is now one profile among many, not the shape of the engine)

## 1. Purpose

Build a generic, runtime **custom rule engine** — risk scoring for the
trading system is the first use case built on it, not the shape of the
engine itself. Anyone should be able to define a rule such as "give me
tickers with high relative volume" using the same registry, validation,
storage, and execution pipeline used for risk, with zero new backend code.

- Indicators/attributes are calculated/provided by existing application services.
- Only indicators registered in an **Indicator Registry** can be used in rules.
- Only outputs registered in an **Output Variable Registry** (§43a) can be
  written by rules — this is what keeps "risk score" from being a special
  case baked into the engine.
- Users create and modify rules from the UI without changing Java code.
- Rules are stored as data/configuration in the database.
- The backend validates and interprets the rule definitions.
- Drools is used as the rule execution engine.
- Decision state is created at runtime and is **not required to be persisted**.
- Every decision can explain which rules fired and how the final value(s) were calculated.
- New indicators can be added later by registering the indicator and implementing only its data provider; existing rule infrastructure remains unchanged.
- New **kinds of rules** (screeners, classifiers, transformers — not just
  accumulators like risk) require no new tables and no new engine code —
  only a new profile with an `evaluation_mode` (§42) and declared output
  variables (§43a).

See §3a for exactly how a rule stops being "a risk thing" and becomes "a
thing rules do."

---

# 2. High-Level Architecture

```text
                         ┌──────────────────────┐
                         │      Angular UI      │
                         │                      │
                         │ Indicator Registry   │
                         │ Rule Builder         │
                         │ Rule List / Version  │
                         │ Rule Test / Preview  │
                         └──────────┬───────────┘
                                    │ REST
                                    ▼
                         ┌──────────────────────┐
                         │ Spring Boot Backend  │
                         │                      │
                         │ Rule Management API  │
                         │ Rule Validation      │
                         │ Rule Compiler        │
                         │ Decision Service     │
                         └──────────┬───────────┘
                                    │
                  ┌─────────────────┼──────────────────┐
                  │                 │                  │
                  ▼                 ▼                  ▼
          ┌──────────────┐  ┌──────────────┐  ┌────────────────┐
          │ Indicator DB │  │  Rule DB     │  │ Existing       │
          │ / Registry   │  │              │  │ Indicator APIs │
          └──────────────┘  └──────────────┘  └───────┬────────┘
                                                       │
                                                       ▼
                                              ┌────────────────┐
                                              │ SubjectContext  │
                                              │ RSI            │
                                              │ ATR            │
                                              │ PVPP           │
                                              │ RVOL           │
                                              │ MK             │
                                              │ VCP            │
                                              │ ...            │
                                              └───────┬────────┘
                                                      │
                                                      ▼
                                              ┌────────────────┐
                                              │ Drools Engine  │
                                              │                │
                                              │ Conditions     │
                                              │ Calculations   │
                                              │ Actions        │
                                              └───────┬────────┘
                                                      │
                                                      ▼
                                              ┌────────────────┐
                                              │ Runtime State  │
                                              │ RiskScore      │
                                              │ Decision       │
                                              │ Explanation    │
                                              └────────────────┘
```

---

# 3. Core Design Principle

The system must separate four concerns:

```text
Indicator
    ↓
Value Provider
    ↓
Rule Definition
    ↓
Rule Execution
```

An indicator should **not contain trading/risk logic**.

For example:

```text
RSI
 ↓
RSI Provider
 ↓
Returns RSI / RSI Percentile
 ↓
Rule Engine
 ↓
Rules decide what RSI means
```

This allows the same RSI value to be used by many independent rules.

---

# 3a. Generalizing Beyond Risk

The original design already got the hard part right: indicators are
decoupled from trading logic via the provider abstraction, and rules are
data, not code. The one place risk leaked into the engine itself was the
**output side** — a single implicit `RiskState.score`, and action types
like `INCREASE_RISK`. Fix that one seam and the rest of the architecture
(registry, validation, versioning, Drools compilation, UI rule builder)
needs no changes to support any other kind of rule.

Three renames make this concrete, mapping the risk-specific concept to its
generalized counterpart used throughout the rest of this document:

| Risk-specific (before) | Generalized (this document, after) |
|---|---|
| `MarketContext { ticker, date, indicators }` | `SubjectContext { subjectType, subjectId, attributes }` (§6) — a subject can be a ticker, a portfolio, an order, a user, anything with registered attribute providers |
| `RiskState { score }` | `DecisionState { outputs: Map<String,Object> }` (§7, §15) — driven by the `decision_output_variable` registry (§43a), so "risk score" is just one output someone configured |
| `action_type: INCREASE_RISK / DECREASE_RISK` | `action_type: SET / INCREASE / DECREASE / TAG / SELECT / EXCLUDE / EMIT_EVENT` (§10) — `SELECT`/`EXCLUDE` are what a screener uses, `TAG` is what a classifier uses |

With that in place, "risk calculator" and "high-volume screener" are two
**profiles** (§42) on the same engine, distinguished only by
`evaluation_mode` and which output variables they declare — not by any
code path:

```text
Profile: RISK_ADJUSTMENT              Profile: HIGH_VOLUME_SCREEN
mode = ACCUMULATE                     mode = FILTER
output: RISK_SCORE (NUMBER, init 100) output: MATCHED (BOOLEAN, init false)

Rule: RSI_PERCENTILE >= 0             Rule: RVOL > 3
 → DECREASE RISK_SCORE BY (...)        → SET MATCHED = true
```

Running a `FILTER`-mode profile against a universe of tickers and
collecting the ones where `MATCHED = true` is exactly "get me tickers with
high volume" — same registry, same validation pipeline, same Drools
compilation, same UI rule builder, different profile config. See §19a for
how this runs efficiently across many subjects at once.

---

# 4. Indicator Registry

The Indicator Registry defines which indicators are available for rule creation.

This is the source of truth for the UI.

## Example table: `decision_indicator`

| Column | Type | Description |
|---|---|---|
| id | BIGINT | Primary key |
| code | VARCHAR | Unique technical identifier |
| name | VARCHAR | Display name |
| category | VARCHAR | Indicator category |
| value_type | VARCHAR | NUMBER / BOOLEAN / STRING |
| unit | VARCHAR | %, score, ratio, etc. |
| source_type | VARCHAR | DB / SERVICE / CALCULATED |
| source_reference | VARCHAR | Provider/service identifier |
| enabled | BOOLEAN | Can be used in rules |
| min_value | DECIMAL | Optional minimum |
| max_value | DECIMAL | Optional maximum |
| description | TEXT | UI help text |
| created_at | DATETIME | Created time |
| updated_at | DATETIME | Updated time |

## Example records

| code | name | category | value_type | source_type | enabled |
|---|---|---|---|---|---|
| RSI | RSI | MOMENTUM | NUMBER | SERVICE | true |
| RSI_PERCENTILE | RSI Percentile | MOMENTUM | NUMBER | SERVICE | true |
| ATR | ATR | VOLATILITY | NUMBER | DB | true |
| ATR_PERCENTILE | ATR Percentile | VOLATILITY | NUMBER | CALCULATED | true |
| PVPP | PVPP Score | PRICE_VOLUME | NUMBER | DB | true |
| RVOL | Relative Volume | VOLUME | NUMBER | DB | true |
| MK_TAU | Mann-Kendall Tau | TREND | NUMBER | DB | true |
| MK_SLOPE | Mann-Kendall Slope | TREND | NUMBER | DB | true |
| VCP | VCP Signal | PATTERN | BOOLEAN | SERVICE | true |
| SORTINO | Sortino Ratio | RISK | NUMBER | DB | true |
| DELIVERY_PERCENT | Delivery % | VOLUME | NUMBER | DB | true |

The UI should only show indicators where:

```text
enabled = true
```

Therefore, if `RSI_PERCENTILE` exists in the registry, the user can select it while creating a rule.

---

# 5. Indicator Provider Abstraction

The engine should not directly know where an indicator comes from, and it
should not care whether that indicator is a simple per-subject calculation
or a heavier method that scans a whole universe at once — both are just
`IndicatorProvider` implementations.

This is the **one, canonical definition** of the interface. It is used
everywhere else in this document — §43a, §46, §65c, and the concrete
examples all refer back to this shape rather than redefining it.

```java
public interface IndicatorProvider {

    String getIndicatorCode();

    // Declares this provider's own configurable inputs (window sizes,
    // thresholds, date ranges) — independent of which subject is being
    // evaluated. Return an empty list for indicators that take no
    // configuration at all.
    List<ParamSpec> getParameterSpecs();

    // Resolves this indicator's value for every subject in the given set,
    // using params already resolved from decision_rule_condition
    // (§46) — constants substituted, expressions evaluated.
    //
    // A simple per-subject indicator (RSI, ATR) just loops and computes
    // one value per subject. A provider backed by an existing batch/range
    // method (e.g. a consistent-volume detector, §65c) makes ONE call
    // across the whole subject set and returns a map keyed by subjectId —
    // callers don't need to know or care which style a given provider is.
    Map<String, Object> resolveValues(
        List<SubjectContext> subjects,
        Map<String, Object> resolvedParams
    );
}
```

```java
public record ParamSpec(
    String name,
    ParamType type,                  // INTEGER / DOUBLE / STRING / LOCAL_DATE / BOOLEAN
    ParamSourceType defaultSource,   // CONSTANT / EXPRESSION
    String defaultExpression         // only used when defaultSource = EXPRESSION
) {}
```

`ParamSourceType.EXPRESSION` values (e.g. `today().minusMonths(18)`) are
evaluated once per rule execution via SpEL, not baked into a stored date —
see §65c for the expression-evaluation mechanics and where a specific
rule's chosen parameter values are stored.

Two examples, showing both shapes side by side:

```java
@Component
public class RsiProvider implements IndicatorProvider {

    public String getIndicatorCode() { return "RSI"; }

    public List<ParamSpec> getParameterSpecs() {
        return List.of(
            new ParamSpec("period", INTEGER, CONSTANT, "14")
        );
    }

    public Map<String, Object> resolveValues(
            List<SubjectContext> subjects, Map<String, Object> params) {

        Map<String, Object> values = new HashMap<>();
        for (SubjectContext s : subjects) {
            values.put(s.getSubjectId(), computeRsi(s, (int) params.get("period")));
        }
        return values;
    }
}
```

```java
@Component
public class ConsistentVolumeProvider implements IndicatorProvider {

    private final ConsistentVolumeService service; // existing bean, unchanged

    public String getIndicatorCode() { return "CONSISTENT_VOLUME"; }

    public List<ParamSpec> getParameterSpecs() {
        return List.of(
            new ParamSpec("fromDate", LOCAL_DATE, EXPRESSION, "today().minusMonths(18)"),
            new ParamSpec("toDate",   LOCAL_DATE, EXPRESSION, "today()"),
            new ParamSpec("inputBaselineWindow",      INTEGER, CONSTANT, null),
            new ParamSpec("baselineLowPercentile",    DOUBLE,  CONSTANT, null),
            new ParamSpec("baselineHighPercentile",   DOUBLE,  CONSTANT, null),
            new ParamSpec("baseRvolPercentileWindow", INTEGER, CONSTANT, null),
            new ParamSpec("rvolThresholdPercentile",  DOUBLE,  CONSTANT, null),
            new ParamSpec("consistencyWindow",        INTEGER, CONSTANT, null),
            new ParamSpec("requiredScore",            INTEGER, CONSTANT, null)
        );
    }

    public Map<String, Object> resolveValues(
            List<SubjectContext> subjects, Map<String, Object> params) {

        // One call across the whole set — not one per subject.
        List<ConsistentVolumeSignalResponse> matches = service.detectConsistentVolumes(
            (LocalDate) params.get("fromDate"),
            (LocalDate) params.get("toDate"),
            (int)    params.get("inputBaselineWindow"),
            (double) params.get("baselineLowPercentile"),
            (double) params.get("baselineHighPercentile"),
            (int)    params.get("baseRvolPercentileWindow"),
            (double) params.get("rvolThresholdPercentile"),
            (int)    params.get("consistencyWindow"),
            (int)    params.get("requiredScore")
        );

        Set<String> matchedIds = matches.stream()
            .map(ConsistentVolumeSignalResponse::getTicker)
            .collect(Collectors.toSet());

        Map<String, Object> values = new HashMap<>();
        for (SubjectContext s : subjects) {
            values.put(s.getSubjectId(), matchedIds.contains(s.getSubjectId()));
        }
        return values;
    }
}
```

Once registered, `CONSISTENT_VOLUME` is used in a rule condition exactly
like `VCP` or any other boolean indicator — `CONSISTENT_VOLUME == true` —
with no special rule type, no separate execution path, and no engine code
that knows this indicator is "different." See §65c for how a rule chooses
which of `ConsistentVolumeProvider`'s parameters to override.

Other examples of providers, spanning both styles above:

```text
RsiProvider              -- per-subject
AtrProvider              -- per-subject
PvppProvider             -- per-subject
RvolProvider             -- per-subject
MannKendallProvider      -- per-subject
VcpProvider              -- per-subject
ConsistentVolumeProvider -- batch, wraps an existing multi-parameter method
```

Later, adding a new indicator — of either style — requires:

1. Register its code in the indicator registry (§43).
2. Implement one `IndicatorProvider`, declaring `getParameterSpecs()` if
   it takes configuration.
3. The UI automatically exposes it to the rule builder, including a form
   for any declared parameters.

The rule engine itself never needs indicator-specific code — not for RSI,
and not for a heavier batch method either.

---

# 6. Runtime Subject Context

Before executing rules, build a runtime context.

`SubjectContext` is deliberately **not** tied to tickers/dates. A "subject" is
whatever the rule is being evaluated against — a ticker on a date, a
portfolio, an order, a user, a device, anything with registered attribute
providers.

```java
public class SubjectContext {

    private String subjectType;   // e.g. "TICKER", "PORTFOLIO", "ORDER"
    private String subjectId;     // e.g. "RELIANCE", "PORT-1042"
    private LocalDate asOfDate;   // optional, only relevant when time matters

    private Map<String, Object> attributes; // indicator/attribute values
}
```

Example runtime data (risk use case):

```json
{
  "subjectType": "TICKER",
  "subjectId": "RELIANCE",
  "asOfDate": "2026-09-06",
  "attributes": {
    "RSI": 72.4,
    "RSI_PERCENTILE": 90,
    "ATR": 4.2,
    "ATR_PERCENTILE": 82,
    "PVPP": 0.0032,
    "RVOL": 1.8,
    "MK_TAU": 0.71,
    "VCP": true
  }
}
```

Example runtime data (screener use case — no risk concept involved at all):

```json
{
  "subjectType": "TICKER",
  "subjectId": "TATASTEEL",
  "asOfDate": "2026-09-06",
  "attributes": {
    "RVOL": 4.1,
    "DELIVERY_PERCENT": 62.3
  }
}
```

The important point is that this is **runtime data**. The rule definitions
never contain the actual attribute value, and nothing about `SubjectContext`
assumes trading, tickers, or risk — those are just the first data providers
plugged into it.

---

# 7. Runtime Decision State

The engine starts every evaluation with a **generic, named set of outputs**,
not a single hard-coded risk number. "Risk" is simply one output someone
configures — a screener rule might have zero numeric outputs and instead
mark the subject as `MATCHED = true`.

Each named output is declared once in the `decision_output_variable`
registry (see §47a) with a type and, if numeric, an initial value and
optional bounds. Example declarations for two different profiles:

```text
Profile "Risk Adjustment"     → output "RISK_SCORE"   (NUMBER, initial 100)
Profile "High Volume Screen"  → output "MATCHED"      (BOOLEAN, initial false)
Profile "Setup Quality"       → output "QUALITY_TAG"  (STRING,  initial null)
```

```java
DecisionState state = DecisionState.initFor(profile); // seeds all declared outputs
```

The database does not need to contain these values.

During execution (risk example):

```text
Initial RISK_SCORE = 100
        ↓
Rule 1 modifies RISK_SCORE
        ↓
Rule 2 modifies RISK_SCORE
        ↓
Rule 3 modifies RISK_SCORE
        ↓
Final RISK_SCORE
```

During execution (screener example — no accumulation, just a flag):

```text
Initial MATCHED = false
        ↓
Rule: RVOL > 3 → SET MATCHED = true
        ↓
Final MATCHED = true → subject included in result set
```

After the decision is returned, the runtime object can be discarded.

If historical auditing is required later, the final decision and rule execution details can optionally be persisted.

---

# 8. Rule Model

Rules should be stored as structured data, not as raw DRL entered by users.

A rule should contain:

```text
Rule
 ├── Metadata
 ├── Conditions
 ├── Calculation / Action
 ├── Priority
 ├── Execution behavior
 └── Enabled / Version
```

## Example `decision_rule`

| Column | Description |
|---|---|
| id | Rule ID |
| name | Human-readable name |
| rule_type | RISK / SIGNAL / POSITION / STOP_LOSS |
| priority | Execution/conflict priority |
| enabled | Rule enabled |
| status | DRAFT / ACTIVE / DISABLED |
| version | Rule version |
| created_by | User |
| created_at | Timestamp |
| updated_at | Timestamp |

---

# 9. Rule Conditions

Conditions should be represented separately.

Example table:

`decision_rule_condition`

| Column | Description |
|---|---|
| id | Condition ID |
| rule_id | Parent rule |
| indicator_code | RSI, ATR, PVPP, etc. |
| operator | >, >=, <, <=, =, BETWEEN |
| comparison_type | CONSTANT / INDICATOR |
| comparison_value | Constant value |
| comparison_indicator | Optional second indicator |
| logical_operator | AND / OR |

Example:

```text
RSI_PERCENTILE >= 90
```

or:

```text
ATR_PERCENTILE > 80
AND
PVPP > 0.002
```

---

# 10. Rule Actions / Calculations

A rule can perform an action against **any declared output variable**
(§47a), not just risk. `action_type` is generic; `target` says which output
it applies to.

Numeric / accumulator actions (used by RISK, POSITION, etc. profiles):

```text
SET
INCREASE
DECREASE
```

Classification actions (used by SIGNAL/CLASSIFY profiles):

```text
TAG            -- set a string output, e.g. QUALITY_TAG = "STRONG"
```

Filtering / selection actions (used by screener / FILTER profiles — this is
what powers "get me tickers with high volume"):

```text
SELECT         -- include this subject in the result set
EXCLUDE        -- drop this subject from the result set
```

Side-effect / integration actions (optional, for wiring into other systems):

```text
EMIT_EVENT     -- publish a domain event, e.g. "ALERT_TRIGGERED"
```

Trading-specific actions remain available as ordinary output values rather
than special engine concepts — e.g. a SIGNAL profile with output
`ACTION_SIGNAL` (STRING) that rules `SET` to `BUY` / `SELL` / `HOLD` /
`REJECT`, and a STOP_LOSS profile with output `STOP_LOSS_PRICE` (NUMBER).
Nothing in the engine needs to know these strings — they are just values of
a declared output, same as `RISK_SCORE` or `MATCHED`.

For dynamic calculations, support expressions.

Examples:

```text
100 - RSI_PERCENTILE
ATR * 5
RSI_PERCENTILE * 0.5
PVPP * 1000
(100 - RSI_PERCENTILE) * 0.5
```

---

# 11. UI Rule Builder

The user should never need to know Drools syntax.

Example UI:

```text
CREATE RULE

Rule Name:
[ RSI Percentile Risk Adjustment ]

Rule Type:
[ RISK ]

WHEN:

[ RSI Percentile ▼ ] [ >= ▼ ] [ 0 ]

THEN:

Action:
[ DECREASE RISK BY ▼ ]

Calculation:
[ 100 ] [ - ] [ RSI Percentile ▼ ]

Initial Risk:
100

Preview:
RSI Percentile = 90
Adjustment = 100 - 90 = 10
Final Risk = 100 - 10 = 90

[ SAVE ]
```

The user has created:

```text
IF RSI_PERCENTILE >= 0
THEN
    DECREASE RISK BY (100 - RSI_PERCENTILE)
```

---

# 12. Example: RSI Rule

Desired business rule:

> If RSI is at the 90th percentile, reduce the current risk by 10 because 100 - 90 = 10.

The UI representation:

```text
WHEN

Indicator:
RSI_PERCENTILE

Operator:
>=

Value:
0

THEN

Action:
DECREASE_RISK

Formula:
100 - RSI_PERCENTILE
```

At runtime:

```text
RSI_PERCENTILE = 90

Formula:
100 - 90 = 10

Initial Risk:
100

Final Risk:
100 - 10 = 90
```

If RSI percentile is 75:

```text
100 - 75 = 25

Risk:
100 - 25 = 75
```

---

# 13. Example: PVPP Rule

Suppose:

```text
IF PVPP > 0.002
THEN DECREASE RISK BY 15
```

UI:

```text
WHEN

PVPP > 0.002

THEN

DECREASE RISK BY 15
```

At runtime:

```text
Initial Risk = 90

PVPP = 0.0032
Rule fires

Risk = 90 - 15

Risk = 75
```

---

# 14. Multiple Rules

Example:

```text
Initial Risk = 100

Rule 1:
RSI percentile = 90
→ decrease by 100 - 90
→ -10

Risk = 90

Rule 2:
PVPP > 0.002
→ decrease by 15

Risk = 75

Rule 3:
ATR > 4
→ increase by 20

Risk = 95
```

Final:

```text
Risk = 95
```

The important point is that every rule works against the **same runtime DecisionState**.

---

# 15. DecisionState

`DecisionState` holds **any number of named outputs**, not just a single
risk score. This is what makes the same engine usable for accumulation
(risk), classification (tags), and filtering (matched/not matched) without
touching the runtime.

```java
public class DecisionState {

    private final Map<String, Object> outputs = new HashMap<>();
    private final Map<String, OutputVariableDef> definitions;

    public DecisionState(Map<String, OutputVariableDef> definitions) {
        this.definitions = definitions;
        definitions.forEach((name, def) -> outputs.put(name, def.getInitialValue()));
    }

    public void increase(String output, double value) {
        outputs.merge(output, value, (a, b) -> ((Number) a).doubleValue() + (Double) b);
    }

    public void decrease(String output, double value) {
        outputs.merge(output, -value, (a, b) -> ((Number) a).doubleValue() + (Double) b);
    }

    public void set(String output, Object value) {
        outputs.put(output, value);
    }

    public void tag(String output, String label) {
        outputs.put(output, label);
    }

    public Object get(String output) {
        return outputs.get(output);
    }

    public Map<String, Object> getAll() {
        return outputs;
    }
}
```

For the common single-score risk case, a thin convenience wrapper can still
expose `getScore()` by reading `get("RISK_SCORE")`, so existing risk-only
code does not need to change:

```java
public double getScore() {
    return ((Number) get("RISK_SCORE")).doubleValue();
}
```

Optional bounds:

```java
public void normalize() {
    score = Math.max(0, Math.min(100, score));
}
```

This ensures:

```text
Risk < 0  → 0
Risk > 100 → 100
```

---

# 16. How Rules Are Interpreted

Do NOT directly execute arbitrary user-generated Java or DRL.

Use this pipeline:

```text
UI Rule Builder
      ↓
Rule Definition JSON
      ↓
Backend Validation
      ↓
Rule Normalization
      ↓
Rule Compiler / Interpreter
      ↓
Drools
      ↓
Runtime Execution
```

Example UI JSON:

```json
{
  "name": "RSI Percentile Risk",
  "ruleType": "RISK",
  "conditions": [
    {
      "indicator": "RSI_PERCENTILE",
      "operator": ">=",
      "value": 0
    }
  ],
  "action": {
    "type": "DECREASE_RISK",
    "expression": {
      "operator": "SUBTRACT",
      "left": 100,
      "right": {
        "indicator": "RSI_PERCENTILE"
      }
    }
  }
}
```

The backend validates:

```text
Does RSI_PERCENTILE exist?
Is it enabled?
Is it numeric?
Is >= valid for the data type?
Is the formula valid?
Are all referenced indicators available?
Is the expression safe?
```

Only then should it become executable.

---

# 17. Drools Integration

Drools receives:

```text
SubjectContext
DecisionState
DecisionContext
```

Rules operate on those runtime facts.

Conceptually:

```drools
rule "RSI Percentile Risk"
when
    $m : SubjectContext()
    $r : DecisionState()
then
    double adjustment =
        100 - $m.getIndicator("RSI_PERCENTILE");

    $r.decrease(adjustment);
end
```

The application should preferably generate/compile these rules from the validated domain rule model rather than allowing users to write DRL.

---

# 18. Rule Execution Service

A central service should orchestrate the complete process. This is the
single-subject entry point — "evaluate this one ticker." For running the
same profile against several specific tickers, a whole watchlist, or the
entire universe in one call, see the batch flow in §19a and the API-level
`scope` selection in §65a.

```java
DecisionResult evaluate(
    String subjectType,
    String subjectId,
    LocalDate asOfDate,
    DecisionConfig config
)
```

Processing:

```text
1. Load active rules
2. Determine required indicators
3. Load/calculate indicator values
4. Build SubjectContext
5. Create DecisionState(initial = 100)
6. Insert facts into Drools
7. Fire rules
8. Apply final risk bounds
9. Build DecisionResult
10. Return result
```

---

# 19. Only Fetch Indicators Required by Active Rules

This is important for performance.

Suppose active rules use:

```text
RSI_PERCENTILE
PVPP
ATR
```

but not:

```text
RVOL
MK
VCP
SORTINO
```

The engine should not unnecessarily calculate/load every indicator.

Rule analysis can determine:

```text
Required Indicators:
RSI_PERCENTILE
PVPP
ATR
```

Then only these providers are called.

This will be especially useful when evaluating thousands of tickers.

---

# 19a. Batch / Universe Evaluation

The single-subject flow (§18, `evaluate(subjectType, subjectId, asOfDate,
config)`) answers "what is the outcome for this one subject." Running
against several specific tickers, a saved watchlist, or the whole universe
is the same evaluation logic applied to a set instead of one subject — see
§65a for how a caller specifies exactly which set via the API-level `scope`
object.

Add a batch entry point alongside the existing single-subject one:

```java
List<DecisionResult> evaluateBatch(
    List<SubjectContext> subjects,
    DecisionProfile profile
)
```

Processing, reusing everything already defined:

```text
1. Load active rules for the profile (same as §18)
2. Determine required indicators (§19) — computed once, not per subject
3. For each subject in the batch:
     a. Load/attach only the required indicator values
     b. Build SubjectContext (§6)
     c. Initialize DecisionState from decision_output_variable (§43a)
     d. Insert facts, fire rules, collect DecisionResult
4. If profile.evaluation_mode == FILTER:
     Return only subjects where the declared boolean output is true
   Else:
     Return all DecisionResults
```

This is the exact mechanism behind "get me tickers with high volume":

```text
Profile: HIGH_VOLUME_SCREEN (mode = FILTER)
Rule: RVOL > 3 → SET MATCHED = true

evaluateBatch(all_tickers_today, HIGH_VOLUME_SCREEN)
   → filters internally to results where MATCHED = true
   → returns just those tickers
```

Because required-indicator discovery (§19) already runs once per profile,
batching does not multiply provider calls per subject beyond what each
subject actually needs — this is what makes it viable across thousands of
tickers, not just one at a time.

---

# 20. Rule Dependency Discovery

The backend should expose an endpoint such as:

```text
GET /api/decision/indicators
```

Response:

```json
[
  {
    "code": "RSI_PERCENTILE",
    "name": "RSI Percentile",
    "type": "NUMBER",
    "enabled": true
  },
  {
    "code": "PVPP",
    "name": "PVPP Score",
    "type": "NUMBER",
    "enabled": true
  }
]
```

The UI uses this response to populate the indicator dropdown.

Therefore:

```text
Add indicator to registry
        ↓
Indicator appears in UI
        ↓
User can use it in rules
```

---

# 21. Adding RSI Later

The intended development flow is:

### Step 1 — Register RSI

```text
decision_indicator

code = RSI
name = RSI
type = NUMBER
enabled = true
```

or:

```text
code = RSI_PERCENTILE
name = RSI Percentile
type = NUMBER
enabled = true
```

### Step 2 — Provide RSI data

Implement/connect:

```text
RsiProvider
```

which knows how to retrieve/calculate RSI.

### Step 3 — No rule-engine changes

The generic rule engine automatically understands:

```text
RSI_PERCENTILE
```

because it is registered as an available numeric indicator.

### Step 4 — UI automatically shows RSI

The rule builder displays:

```text
[ RSI Percentile ▼ ]
```

### Step 5 — User creates rules

For example:

```text
RSI Percentile >= 90
→ Decrease Risk By 10
```

or:

```text
Risk Adjustment =
100 - RSI Percentile
```

No new Drools engine logic is required.

---

# 22. Rule Types

The same framework should support different decision types.

```text
RISK
SIGNAL
POSITION_SIZE
STOP_LOSS
TARGET
ENTRY
EXIT
```

Example:

```text
Rule Type = RISK

RSI percentile >= 90
→ Risk - (100 - RSI percentile)
```

Another:

```text
Rule Type = ENTRY

RSI percentile > 80
AND
PVPP > 0.002
AND
MK_TAU > 0.6

→ BUY
```

Another:

```text
Rule Type = POSITION_SIZE

ATR percentile > 90

→ Position Multiplier = 0.5
```

---

# 23. Rule Priority and Conflict Resolution

Multiple rules can fire simultaneously.

Therefore every rule should have:

```text
priority
```

Example:

| Rule | Condition | Action | Priority |
|---|---|---|---:|
| RSI | RSI %ile > 60 | Risk - 10 | 10 |
| RSI Extreme | RSI %ile > 90 | Risk - 25 | 20 |
| ATR Extreme | ATR %ile > 95 | REJECT | 100 |

High-priority rules should be able to override or block lower-priority decisions where appropriate.

The exact conflict policy should be explicitly configured.

Possible policies:

```text
APPLY_ALL
HIGHEST_PRIORITY_ONLY
FIRST_MATCH
LAST_MATCH
MAX_EFFECT
MIN_EFFECT
OVERRIDE
```

For risk adjustments, `APPLY_ALL` may be appropriate.

For mutually exclusive signals, `HIGHEST_PRIORITY_ONLY` or `OVERRIDE` may be appropriate.

---

# 24. Explanation / Audit Output

Every evaluation should produce an explanation.

Example:

```json
{
  "ticker": "RELIANCE",
  "initialRisk": 100,
  "finalRisk": 75,
  "rulesFired": [
    {
      "rule": "RSI Percentile Risk",
      "input": 90,
      "calculation": "100 - 90",
      "adjustment": -10
    },
    {
      "rule": "Strong PVPP",
      "input": 0.0032,
      "condition": "PVPP > 0.002",
      "adjustment": -15
    },
    {
      "rule": "High ATR",
      "input": 4.2,
      "condition": "ATR > 4",
      "adjustment": 20
    }
  ]
}
```

This makes the engine explainable.

---

# 25. Rule Testing / Preview

The UI should include a test feature.

User selects:

```text
Ticker:
RELIANCE

Date:
2026-09-06
```

The system retrieves:

```text
RSI percentile = 90
PVPP = 0.0032
ATR = 4.2
```

Then shows:

```text
Initial Risk                    100

RSI Rule
100 - 90 = 10
Risk                           90

PVPP Rule
-15
Risk                           75

ATR Rule
+20
Risk                           95

FINAL RISK                     95
```

This allows rules to be validated before activation.

---

# 26. Rule Lifecycle

Rules should have lifecycle states:

```text
DRAFT
   ↓
VALIDATED
   ↓
ACTIVE
   ↓
DISABLED
   ↓
ARCHIVED
```

A rule should not become active until validation succeeds.

---

# 27. Rule Versioning

Do not overwrite active rules blindly.

Example:

```text
RSI Risk Rule v1
RSI >= 90 → -10

RSI Risk Rule v2
RSI >= 90 → -(100 - RSI percentile)
```

Store versions so that decisions can be reproduced later.

This is important for backtesting and production debugging.

---

# 28. Backtesting Compatibility

The same decision engine should be usable for:

```text
Live Trading
Backtesting
Historical Analysis
Paper Trading
Rule Simulation
```

The only major difference should be the source of `SubjectContext`.

```text
Historical Data → SubjectContext → Decision Engine
Live Data       → SubjectContext → Decision Engine
```

This prevents the live logic and backtest logic from diverging.

---

# 29. Recommended Database Structure

Minimum baseline:

```text
decision_indicator
decision_rule
decision_rule_condition
decision_rule_action
decision_rule_version
```

Optional later:

```text
decision_rule_execution
decision_audit
decision_profile
decision_parameter
```

Suggested relationships:

```text
decision_indicator
       │
       │ referenced by
       ▼
decision_rule_condition
       │
       ▼
decision_rule
       │
       ▼
decision_rule_action
```

---

# 30. Decision Profile

A useful future feature is grouping rules into profiles.

Example:

```text
Profile:
Momentum Trading

Rules:
- RSI Risk
- PVPP Risk
- ATR Risk
- MK Trend
- VCP Confirmation
```

Another:

```text
Profile:
Swing Trading

Rules:
- RSI Risk
- ATR Risk
- VCP
- Delivery
```

The runtime request can specify:

```text
profile = MOMENTUM
```

and the engine loads only active rules belonging to that profile.

---

# 31. Recommended Processing Flow

```text
                START
                  │
                  ▼
        Load Decision Profile
                  │
                  ▼
          Load Active Rules
                  │
                  ▼
       Extract Required Indicators
                  │
                  ▼
       Load/Calculate Indicator Data
                  │
                  ▼
           Build SubjectContext
                  │
                  ▼
        Create Runtime DecisionState
              Risk = 100
                  │
                  ▼
           Validate Context
                  │
                  ▼
            Execute Drools
                  │
          ┌───────┴────────┐
          │                │
       Rule fires       Rule doesn't fire
          │                │
          ▼                ▼
    Modify Runtime       No change
        State
          │
          └───────┬────────┘
                  ▼
          Resolve Conflicts
                  │
                  ▼
         Apply Risk Boundaries
             0 <= Risk <= 100
                  │
                  ▼
         Generate Explanation
                  │
                  ▼
          Generate Decision
                  │
                  ▼
                 END
```

---

# 32. Example End-to-End

Suppose the UI creates three rules.

### Rule 1

```text
WHEN RSI_PERCENTILE >= 0

DECREASE RISK BY:
100 - RSI_PERCENTILE
```

### Rule 2

```text
WHEN PVPP > 0.002

DECREASE RISK BY:
15
```

### Rule 3

```text
WHEN ATR > 4

INCREASE RISK BY:
20
```

Runtime data:

```text
RSI_PERCENTILE = 90
PVPP = 0.0032
ATR = 4.2
```

Execution:

```text
Initial Risk
100

RSI Rule
100 - 90 = 10
100 - 10 = 90

PVPP Rule
90 - 15 = 75

ATR Rule
75 + 20 = 95

Final Risk
95
```

No `risk_score` column is necessary.

---

# 33. Important Safety / Validation Rules

The backend must validate:

- Indicator exists.
- Indicator is enabled.
- Indicator data type supports the requested operation.
- Formula contains only allowed operators/functions.
- No arbitrary Java execution.
- No arbitrary SQL.
- No circular rule dependencies.
- No invalid mathematical operations.
- Numeric values are within configured limits.
- Risk is bounded.
- Rule version is valid.
- Rule is associated with a valid decision profile.
- User has permission to activate the rule.

---

# 34. Formula Engine

Drools is responsible for **rule matching and execution**.

A separate safe expression layer can handle dynamic formulas.

Supported initial operators:

```text
+
-
*
/
%
ABS
MIN
MAX
CLAMP
```

Example:

```text
100 - RSI_PERCENTILE
```

```text
ATR * 5
```

```text
MAX(0, 100 - RSI_PERCENTILE)
```

```text
CLAMP(100 - RSI_PERCENTILE, 0, 100)
```

Do not initially support arbitrary scripting.

Keep the formula language deliberately restricted.

---

# 35. Recommended First Version

Do not build every feature at once.

### Phase 1

Build:

```text
Indicator Registry
Rule DB
Rule Condition
Rule Action
Runtime SubjectContext
Runtime DecisionState
Rule Builder UI
Drools integration
Rule validation
Rule execution explanation
```

Support:

```text
>
>=
<
<=
=
BETWEEN

+
-
*
/
```

and:

```text
SET
INCREASE
DECREASE
```

### Phase 2

Add:

```text
AND
OR
Nested conditions
Priority
Rule profiles
Rule versioning
Preview/testing
```

### Phase 3

Add:

```text
Backtesting
Rule performance analysis
Rule simulation
Advanced formulas
Conditional actions
Position sizing
Stop-loss decisions
```

---

# 36. Most Important Architectural Principle

The final system should allow this development workflow:

```text
Developer adds indicator
        ↓
Indicator registered
        ↓
Indicator provider supplies value
        ↓
Indicator automatically appears in UI
        ↓
User creates rules
        ↓
Backend validates rule
        ↓
Rule is compiled/interpreted
        ↓
Drools executes rule
        ↓
Runtime DecisionState changes
        ↓
Final Decision returned
```

Therefore, once the baseline engine is implemented, adding a new indicator such as RSI should primarily require:

```text
1. Implement RSI data provider
2. Register RSI in decision_indicator
```

After that, the UI should automatically allow:

```text
RSI > 60
RSI percentile > 90
RSI BETWEEN 60 AND 70
Risk -= 100 - RSI percentile
Risk += RSI * 0.5
```

without modifying the core decision engine.

---

# 37. Target Outcome

The end goal is a generic **configuration-driven decision engine**:

```text
             INDICATORS
                  │
                  ▼
          ┌───────────────┐
          │ Indicator     │
          │ Registry      │
          └───────┬───────┘
                  │
                  ▼
          ┌───────────────┐
          │ Rule Builder  │
          │ UI            │
          └───────┬───────┘
                  │
                  ▼
          ┌───────────────┐
          │ Rule          │
          │ Definitions   │
          └───────┬───────┘
                  │
                  ▼
          ┌───────────────┐
          │ Validation +  │
          │ Compilation   │
          └───────┬───────┘
                  │
                  ▼
             ┌─────────┐
             │ Drools  │
             └────┬────┘
                  │
                  ▼
        ┌─────────────────────┐
        │ Runtime Decision    │
        │                     │
        │ Risk Score          │
        │ Signal              │
        │ Position Size       │
        │ Stop Loss           │
        │ Explanation         │
        └─────────────────────┘
```

This architecture keeps **indicator calculation, rule definition, rule execution, and final trading decisions decoupled** while allowing the UI to become the primary way of configuring the decision logic.


---

# 38. Technical Implementation Design

This section defines the concrete Spring Boot / Java implementation that should sit behind the architecture above.

## 38.1 Technology Baseline

Recommended baseline:

```text
Java
Spring Boot
Gradle
MySQL
Drools / KIE
REST API
Angular UI
```

The implementation should use the project's existing Spring Boot and Java versions rather than introducing a second framework/version family.

Drools should be isolated behind a `DecisionEngine` service so that the rest of the application does not depend directly on Drools APIs.

---

# 39. Gradle Dependencies

The exact Drools/KIE version must be selected to match the existing Java and Spring Boot version.

The implementation should add the required KIE/Drools modules to `build.gradle`.

Conceptually:

```gradle
dependencies {

    implementation "org.springframework.boot:spring-boot-starter-web"
    implementation "org.springframework.boot:spring-boot-starter-validation"
    implementation "org.springframework.boot:spring-boot-starter-data-jpa"

    // Existing MySQL dependency should be retained
    runtimeOnly "com.mysql:mysql-connector-j"

    // Drools / KIE
    implementation "org.kie:kie-api:<compatible-version>"
    implementation "org.kie:kie-ci:<compatible-version>"
    implementation "org.kie:kie-internal:<compatible-version>"

    // Drools core modules required by the selected KIE version
    implementation "org.drools:drools-core:<compatible-version>"
    implementation "org.drools:drools-compiler:<compatible-version>"

    testImplementation "org.springframework.boot:spring-boot-starter-test"
}
```

### Important

Do not blindly copy versions from this document.

First inspect:

```text
build.gradle
gradle.properties
settings.gradle
gradle-wrapper.properties
```

and select a Drools/KIE release compatible with:

```text
Java version
Spring Boot version
Gradle version
```

Dependency versions should be centralized where possible:

```gradle
ext {
    droolsVersion = "..."
}
```

Then:

```gradle
implementation "org.kie:kie-api:${droolsVersion}"
```

This makes future upgrades easier.

---

# 40. Suggested Package Structure

Use a dedicated module/package for the decision engine.

Example:

```text
src/main/java/com/<company>/<project>/

├── decision/
│   ├── controller/
│   │   ├── DecisionController.java
│   │   ├── DecisionRuleController.java
│   │   └── DecisionIndicatorController.java
│   │
│   ├── service/
│   │   ├── DecisionService.java
│   │   ├── RuleService.java
│   │   ├── IndicatorService.java
│   │   ├── RuleValidationService.java
│   │   ├── RuleCompilationService.java
│   │   └── RuleExecutionService.java
│   │
│   ├── engine/
│   │   ├── DecisionEngine.java
│   │   ├── DroolsDecisionEngine.java
│   │   ├── KieSessionFactory.java
│   │   └── RuleRuntime.java
│   │
│   ├── indicator/
│   │   ├── IndicatorProvider.java
│   │   ├── IndicatorProviderRegistry.java
│   │   ├── RsiIndicatorProvider.java
│   │   ├── AtrIndicatorProvider.java
│   │   └── PvppIndicatorProvider.java
│   │
│   ├── model/
│   │   ├── SubjectContext.java
│   │   ├── DecisionState.java
│   │   ├── DecisionResult.java
│   │   ├── RuleDefinition.java
│   │   ├── RuleCondition.java
│   │   ├── RuleAction.java
│   │   └── FormulaExpression.java
│   │
│   ├── entity/
│   │   ├── DecisionIndicatorEntity.java
│   │   ├── DecisionRuleEntity.java
│   │   ├── DecisionRuleVersionEntity.java
│   │   ├── DecisionRuleConditionEntity.java
│   │   ├── DecisionRuleActionEntity.java
│   │   └── DecisionProfileEntity.java
│   │
│   ├── repository/
│   │   ├── DecisionIndicatorRepository.java
│   │   ├── DecisionRuleRepository.java
│   │   ├── DecisionRuleVersionRepository.java
│   │   └── DecisionProfileRepository.java
│   │
│   └── exception/
│       ├── RuleValidationException.java
│       ├── RuleCompilationException.java
│       └── DecisionExecutionException.java
```

The exact package names should follow the existing project conventions.

---

# 41. Database Schema

MySQL should be used consistently with the existing application's database.

The core schema should contain the following tables.

```text
decision_profile
decision_indicator
decision_output_variable
decision_rule
decision_rule_version
decision_rule_condition
decision_rule_action
```

Optional (needed only if chained/funnel screening — §65b — is required):

```text
decision_pipeline
decision_pipeline_stage
```

No separate table is needed to wrap existing multi-parameter methods
(§65c) — that only adds one column, `indicator_params_json`, to
`decision_rule_condition` (§46).

Optional audit tables:

```text
decision_rule_execution
decision_execution_detail
```

## Schema-wide key conventions

Two changes apply across every table below, compared to a naive
auto-increment-only design:

- **Internal PK vs external ID.** Every table keeps `id BIGINT AUTO_INCREMENT`
  as the internal primary key (cheap joins, clustered index locality). But
  anything returned by a REST API also gets `public_id BINARY(16) NOT NULL`
  (a UUID, generated at insert time) with its own `UNIQUE KEY`. APIs expose
  `public_id`, never the raw `id`. This avoids leaking row counts/insert
  order and lets ids be created client-side (offline drafts, imports)
  without collisions.
- **Ownership scoping.** Because rules are meant to be created by any user
  for any purpose (not just one shared risk configuration), `owner_id`
  (or `workspace_id`, if rules are shared within a team) is a first-class
  column on `decision_profile` and `decision_rule`, and uniqueness of
  human-chosen codes/names is scoped to the owner — not global. Two users
  must both be able to create a rule called `high_volume`.

---

# 42. `decision_profile`

A profile groups rules into a logical decision strategy, and declares
**how** its rules operate on state via `evaluation_mode` — this is what
lets the same tables represent a risk calculator, a screener, or a
classifier.

```sql
CREATE TABLE decision_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    owner_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    evaluation_mode VARCHAR(30) NOT NULL DEFAULT 'ACCUMULATE',
    subject_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_by VARCHAR(100),
    updated_at DATETIME NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_decision_profile_public_id (public_id),
    UNIQUE KEY uk_decision_profile_owner_code (owner_id, code),
    KEY idx_decision_profile_owner (owner_id)
);
```

`evaluation_mode`:

```text
ACCUMULATE   -- running numeric state, e.g. risk score (starts at N, +/- per rule)
FILTER       -- boolean pass/fail per subject, used to build result sets (screeners)
CLASSIFY     -- assigns one or more string tags/labels per subject
TRANSFORM    -- computes one or more derived output values, no accumulation
```

`subject_type` declares what kind of thing this profile evaluates
(`TICKER`, `PORTFOLIO`, `ORDER`, ...) so the UI can offer the right
attribute providers and the engine can validate that every referenced
indicator is actually available for that subject type.

Example profiles:

```text
code=RISK_ADJUSTMENT      mode=ACCUMULATE  subject_type=TICKER
code=HIGH_VOLUME_SCREEN   mode=FILTER      subject_type=TICKER
code=SETUP_QUALITY        mode=CLASSIFY    subject_type=TICKER
```

---

# 43. `decision_indicator`

This is the master registry of indicators/attributes available for rule
creation. It is already subject-agnostic in the original design — the fix
here is purely about keys, not shape.

```sql
CREATE TABLE decision_indicator (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(100) NOT NULL,
    subject_type VARCHAR(50) NOT NULL,
    value_type VARCHAR(30) NOT NULL,
    unit VARCHAR(50),
    source_type VARCHAR(30) NOT NULL,
    source_reference VARCHAR(200),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    min_value DECIMAL(20,8),
    max_value DECIMAL(20,8),
    description TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    PRIMARY KEY (code),
    KEY idx_decision_indicator_enabled (enabled),
    KEY idx_decision_indicator_category (category),
    KEY idx_decision_indicator_subject_type (subject_type)
);
```

`code` is promoted to the primary key here: indicators are a natural-key
entity (looked up by code everywhere, rarely if ever numerically joined at
volume the way rule versions are), so a surrogate `id` just adds an
unnecessary indirection. `subject_type` is added so the registry can hold
attributes for multiple kinds of subjects (`TICKER`, `PORTFOLIO`, ...)
without them colliding.

Example:

```sql
INSERT INTO decision_indicator
(code, name, category, subject_type, value_type, unit, source_type, enabled)
VALUES
('RSI', 'RSI', 'MOMENTUM', 'TICKER', 'NUMBER', 'VALUE', 'SERVICE', TRUE),
('RSI_PERCENTILE', 'RSI Percentile', 'MOMENTUM', 'TICKER', 'NUMBER', 'PERCENTILE', 'SERVICE', TRUE),
('ATR', 'ATR', 'VOLATILITY', 'TICKER', 'NUMBER', 'PERCENT', 'DB', TRUE),
('PVPP', 'PVPP Score', 'PRICE_VOLUME', 'TICKER', 'NUMBER', 'SCORE', 'DB', TRUE),
('RVOL', 'Relative Volume', 'VOLUME', 'TICKER', 'NUMBER', 'RATIO', 'DB', TRUE),
('MK_TAU', 'Mann-Kendall Tau', 'TREND', 'TICKER', 'NUMBER', 'SCORE', 'DB', TRUE),
('VCP', 'VCP', 'PATTERN', 'TICKER', 'BOOLEAN', 'BOOLEAN', 'SERVICE', TRUE);
```

---

# 43a. `decision_output_variable`

New table. This is the registry of things a rule is allowed to write to —
the generalized counterpart of `decision_indicator` (which is what a rule
*reads*). Without this table, `target` on `decision_rule_action` is a bare
string and nothing stops a typo or an undeclared output from silently
doing nothing.

```sql
CREATE TABLE decision_output_variable (
    id BIGINT NOT NULL AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    value_type VARCHAR(30) NOT NULL,        -- NUMBER / BOOLEAN / STRING
    initial_value VARCHAR(200),             -- e.g. '100', 'false', NULL
    min_value DECIMAL(20,8),
    max_value DECIMAL(20,8),
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_output_variable_profile_code (profile_id, code),

    CONSTRAINT fk_output_variable_profile
        FOREIGN KEY (profile_id)
        REFERENCES decision_profile(id)
);
```

Example rows:

```text
profile=RISK_ADJUSTMENT     code=RISK_SCORE   type=NUMBER   initial=100  bounds=[0,100]
profile=HIGH_VOLUME_SCREEN  code=MATCHED      type=BOOLEAN  initial=false
profile=SETUP_QUALITY       code=QUALITY_TAG  type=STRING   initial=NULL
```

`decision_rule_action.target` (§47) should be validated against this table
at rule-save time, the same way conditions are validated against
`decision_indicator`.

---

# 44. `decision_rule`

Stores the logical identity and current status of a rule.

```sql
CREATE TABLE decision_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    owner_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    current_version INT NOT NULL DEFAULT 1,
    description TEXT,
    created_by VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_by VARCHAR(100),
    updated_at DATETIME NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_decision_rule_public_id (public_id),
    UNIQUE KEY uk_decision_rule_owner_code (owner_id, code),
    KEY idx_decision_rule_profile (profile_id),
    KEY idx_decision_rule_status (status),
    KEY idx_decision_rule_enabled (enabled),

    CONSTRAINT fk_decision_rule_profile
        FOREIGN KEY (profile_id)
        REFERENCES decision_profile(id)
);
```

`uk_decision_rule_code` (global) is replaced with `uk_decision_rule_owner_code`
so rule names are unique per owner, not system-wide.

`rule_type` categorizes rules for the UI/rule-builder (e.g. `MOMENTUM`,
`RISK`, `SIGNAL` — free-form, not an engine concept); every rule is built
from `decision_rule_condition`/`decision_rule_action` rows regardless of
`rule_type`, including rules whose conditions reference a parameterized,
batch-backed indicator (§65c) — there is no separate rule shape for that.

Statuses:

```text
DRAFT
VALIDATED
ACTIVE
DISABLED
ARCHIVED
```

---

# 45. `decision_rule_version`

Never modify an active rule in place.

Store immutable versions.

```sql
CREATE TABLE decision_rule_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_id BIGINT NOT NULL,
    version INT NOT NULL,
    definition_json JSON NOT NULL,
    compiled_rule TEXT,
    checksum VARCHAR(128),
    status VARCHAR(30) NOT NULL,
    created_by VARCHAR(100),
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_decision_rule_version (rule_id, version),
    KEY idx_decision_rule_version_rule (rule_id),

    CONSTRAINT fk_decision_rule_version_rule
        FOREIGN KEY (rule_id)
        REFERENCES decision_rule(id)
);
```

`definition_json` should contain the canonical rule representation generated by the UI.

`compiled_rule` is optional. It can store generated DRL if the implementation chooses to persist compiled/generated DRL.

---

# 46. `decision_rule_condition`

```sql
CREATE TABLE decision_rule_condition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_version_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    indicator_code VARCHAR(100) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    comparison_type VARCHAR(30) NOT NULL,
    comparison_value_json JSON,
    comparison_indicator_code VARCHAR(100),
    indicator_params_json JSON,
    logical_operator VARCHAR(10),
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_condition_version_seq (rule_version_id, sequence_no),
    KEY idx_rule_condition_indicator (indicator_code),

    CONSTRAINT fk_rule_condition_version
        FOREIGN KEY (rule_version_id)
        REFERENCES decision_rule_version(id),
    CONSTRAINT fk_rule_condition_indicator
        FOREIGN KEY (indicator_code)
        REFERENCES decision_indicator(code)
);
```

Three changes from the original design:

- `comparison_value DECIMAL(30,12)` → `comparison_value_json JSON`. The
  original column can only hold a number, but `decision_indicator.value_type`
  allows `BOOLEAN` and `STRING` too — a condition like `VCP = true` or
  `SETUP_STATUS = 'CONFIRMED'` had nowhere to store its comparison value.
  One JSON column (`{"type":"NUMBER","value":90}`, `{"type":"BOOLEAN","value":true}`)
  handles all value types without three parallel nullable columns.
- Added `UNIQUE(rule_version_id, sequence_no)` so two conditions can't
  silently collide on ordering, and an explicit FK to `decision_indicator`
  so a typo'd indicator code fails at the database level, not just at
  application-level validation.
- `indicator_params_json` — only populated when `indicator_code` refers to
  a provider that declares parameters via `getParameterSpecs()` (§5); see
  §65c for its shape and how it's resolved at execution time. `NULL` for
  ordinary parameterless indicators like `RSI` or `VCP`.

Examples:

```text
RSI_PERCENTILE >= 90
PVPP > 0.002
ATR_PERCENTILE BETWEEN 80 AND 100
MK_TAU >= 0.6
RVOL > 3          -- screener condition, same table/shape as a risk condition
```

---

# 47. `decision_rule_action`

```sql
CREATE TABLE decision_rule_action (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_version_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    target VARCHAR(100) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    constant_value DECIMAL(30,12),
    expression_json JSON,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_action_version_seq (rule_version_id, sequence_no),
    KEY idx_rule_action_target (target),

    CONSTRAINT fk_rule_action_version
        FOREIGN KEY (rule_version_id)
        REFERENCES decision_rule_version(id)
);
```

`target` now refers to a row in `decision_output_variable` (§43a) rather
than an implicit magic string, and is validated the same way an indicator
reference is validated on the condition side. Added the same
`UNIQUE(rule_version_id, sequence_no)` fix as conditions.

Examples — risk profile:

```text
target = RISK_SCORE
action_type = INCREASE
constant_value = 12
```

```text
target = RISK_SCORE
action_type = DECREASE
expression_json = {
    "operator": "SUBTRACT",
    "left": 100,
    "right": {
        "indicator": "RSI_PERCENTILE"
    }
}
```

Examples — screener profile (no risk concept at all):

```text
target = MATCHED
action_type = SET
constant_value = 1        -- boolean true
```

Examples — classification profile:

```text
target = QUALITY_TAG
action_type = TAG
expression_json = { "operator": "LITERAL", "value": "STRONG" }
```

---

# 48. Optional `decision_rule_execution`

Only create this if execution history is required. Generalized from
`ticker`/`execution_date`/`initial_risk`/`final_risk` to an arbitrary
subject and an arbitrary set of output values, so the same audit table
serves risk runs, screener runs, and anything else.

```sql
CREATE TABLE decision_rule_execution (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    profile_id BIGINT NOT NULL,
    subject_type VARCHAR(50) NOT NULL,
    subject_id VARCHAR(100) NOT NULL,
    as_of_date DATETIME,
    initial_outputs_json JSON,
    final_outputs_json JSON,
    rules_fired_json JSON,
    execution_time_ms BIGINT,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),
    KEY idx_rule_execution_subject (subject_type, subject_id, as_of_date),
    KEY idx_rule_execution_profile (profile_id, created_at),

    CONSTRAINT fk_rule_execution_profile
        FOREIGN KEY (profile_id)
        REFERENCES decision_profile(id)
);
```

For a screener run over thousands of tickers, only matched subjects
typically need a row here — write the executions for `SELECT`ed subjects,
and optionally sample or skip the rest. For high-volume evaluation in
general, avoid storing every execution by default; logging/auditing should
be configurable per profile.

---

# 49. Indicator Provider Interface

See §5 for the canonical `IndicatorProvider` interface and `ParamSpec`
definition — it is not repeated here. Every provider, whether it computes
one value per subject (RSI, ATR) or wraps an existing batch method
(§65c), implements that same interface; there is no separate
numeric/boolean provider hierarchy — a provider's `resolveValues` simply
returns whatever `ParamSpec`-declared value type is appropriate
(`BigDecimal`, `Boolean`, `String`) per subject.

---

# 50. Indicator Provider Registry

Spring can automatically discover providers.

```java
@Component
public class IndicatorProviderRegistry {

    private final Map<String, IndicatorProvider> providers;

    public IndicatorProviderRegistry(
            List<IndicatorProvider> providerList) {

        this.providers = providerList.stream()
            .collect(Collectors.toMap(
                IndicatorProvider::getIndicatorCode,
                Function.identity()
            ));
    }

    public IndicatorProvider get(String code) {
        return providers.get(code);
    }
}
```

Every `IndicatorProvider` bean (§5) — `RsiProvider`, `AtrProvider`,
`ConsistentVolumeProvider`, and any future one — is picked up automatically
this way; adding a new indicator never means touching this registry.

The provider should be responsible only for obtaining the indicator value.

---

# 51. Adding RSI Later

Once the framework is implemented, adding RSI should follow this process:

```text
1. Add RSI provider
2. Register RSI in decision_indicator
3. Ensure source_reference maps to the provider
4. UI automatically displays RSI
5. User creates rules
```

The rule engine itself should not require:

```java
if (indicator.equals("RSI")) {
    ...
}
```

This is critical.

---

# 52. SubjectContext — Implementation Note

`SubjectContext` was already defined canonically in §6. The typed-map
storage shown there (`Map<String, Object> attributes`) is sufficient;
if stronger typing is preferred, split it into
`Map<String, BigDecimal> numericAttributes`,
`Map<String, Boolean> booleanAttributes`, and
`Map<String, String> stringAttributes` internally — either way, the
important property is the same one already stated in §6: no indicator is
hard-coded into the class itself.

---

# 53. DecisionState — Clamping

`DecisionState` was already defined canonically in §15 as a generic named
map, not a single score field. One behavior worth adding there: numeric
outputs should be clamped to the `min_value`/`max_value` bounds declared
on their `decision_output_variable` row (§43a) after every `increase`/
`decrease`/`set` call:

```java
public void clamp(String output) {
    OutputVariableDef def = definitions.get(output);
    if (def.getMinValue() != null || def.getMaxValue() != null) {
        BigDecimal value = (BigDecimal) outputs.get(output);
        if (def.getMinValue() != null) value = value.max(def.getMinValue());
        if (def.getMaxValue() != null) value = value.min(def.getMaxValue());
        outputs.put(output, value);
    }
}
```

For the risk profile specifically, this is what keeps `RISK_SCORE` within
`[0, 100]` as declared in §43a, without the engine needing to know that
`RISK_SCORE` is special — any profile can declare bounds on any numeric
output the same way.

---

# 54. Rule Definition JSON

The canonical representation should be UI-independent.

Example:

```json
{
  "name": "RSI Percentile Risk Adjustment",
  "ruleType": "RISK",
  "priority": 10,
  "conditions": [
    {
      "indicator": "RSI_PERCENTILE",
      "operator": ">=",
      "value": 0
    }
  ],
  "actions": [
    {
      "target": "RISK_SCORE",
      "type": "DECREASE",
      "expression": {
        "operator": "SUBTRACT",
        "left": 100,
        "right": {
          "indicator": "RSI_PERCENTILE"
        }
      }
    }
  ]
}
```

The UI creates this model.

The backend validates it.

The execution engine consumes it.

---

# 55. Formula Expression Model

Expressions should be represented as an AST-like structure rather than raw executable code.

Example:

```json
{
  "operator": "SUBTRACT",
  "left": 100,
  "right": {
    "indicator": "RSI_PERCENTILE"
  }
}
```

More complex:

```json
{
  "operator": "MULTIPLY",
  "left": {
    "operator": "SUBTRACT",
    "left": 100,
    "right": {
      "indicator": "RSI_PERCENTILE"
    }
  },
  "right": 0.5
}
```

This represents:

```text
(100 - RSI_PERCENTILE) * 0.5
```

---

# 56. Formula Evaluation

Do not execute arbitrary Java, JavaScript, SQL or user-supplied code.

Use a restricted expression evaluator.

Allowed baseline:

```text
+
-
*
/
%
ABS
MIN
MAX
CLAMP
```

The evaluator recursively evaluates the expression tree.

Example:

```java
BigDecimal evaluate(
    FormulaExpression expression,
    SubjectContext context
)
```

For:

```text
100 - RSI_PERCENTILE
```

and:

```text
RSI_PERCENTILE = 90
```

the evaluator returns:

```text
10
```

The action layer then applies:

```text
RISK_SCORE -= 10
```

---

# 57. Decision Engine Interface

The rest of the application should depend on an interface.

```java
public interface DecisionEngine {

    DecisionResult evaluate(
        SubjectContext context,
        DecisionRequest request
    );
}
```

Implementation:

```java
@Component
public class DroolsDecisionEngine implements DecisionEngine {
    ...
}
```

This abstraction allows the implementation to change later without changing callers.

---

# 58. Decision Service

```java
@Service
public class DecisionService {

    private final IndicatorContextService indicatorContextService;
    private final RuleService ruleService;
    private final DecisionEngine decisionEngine;

    public DecisionResult evaluate(
            String subjectType,
            String subjectId,
            LocalDate asOfDate,
            String profileCode) {

        DecisionProfile profile =
            ruleService.getActiveProfile(profileCode);

        Set<String> requiredIndicators =
            ruleService.getRequiredIndicators(profile);

        SubjectContext context =
            indicatorContextService.build(
                subjectType,
                subjectId,
                asOfDate,
                requiredIndicators
            );

        return decisionEngine.evaluate(
            context,
            new DecisionRequest(profileCode)
        );
    }
}
```

---

# 59. Required Indicator Discovery

Before executing rules, inspect the active rule definitions.

Example:

```text
Active rules:

Rule A:
RSI_PERCENTILE

Rule B:
PVPP

Rule C:
ATR
MK_TAU
```

Required set:

```text
RSI_PERCENTILE
PVPP
ATR
MK_TAU
```

Only those indicators need to be loaded/calculated.

This is particularly important when processing thousands of tickers.

---

# 60. Drools Runtime Flow

At runtime:

```text
DecisionService
       ↓
Load active rules
       ↓
Discover required indicators
       ↓
Build SubjectContext
       ↓
Create DecisionState
       ↓
Create KIE session
       ↓
Insert SubjectContext
       ↓
Insert DecisionState
       ↓
Insert DecisionContext
       ↓
Fire rules
       ↓
Collect rule execution information
       ↓
Clamp/normalize final risk
       ↓
Create DecisionResult
```

---

# 61. Drools Rule Generation

The UI should not directly generate arbitrary DRL.

The backend should translate the canonical rule model into controlled executable rules.

Conceptually:

```drools
rule "RSI Percentile Risk Adjustment"
salience 10

when
    $context : SubjectContext()
    $risk : DecisionState()
then

    BigDecimal rsiPercentile =
        $context.getNumber("RSI_PERCENTILE");

    BigDecimal adjustment =
        BigDecimal.valueOf(100)
            .subtract(rsiPercentile);

    $risk.decrease(adjustment);
end
```

The generated rule is controlled by the backend.

---

# 62. Alternative: Generic Drools Rule

An alternative architecture is to have a smaller number of generic Drools rules and let the application interpret the rule model.

For example:

```text
RuleDefinition
ConditionEvaluator
ExpressionEvaluator
ActionExecutor
```

Then Drools decides when a rule is applicable while the application handles generic calculations.

This approach reduces the amount of dynamically generated DRL.

The preferred implementation should choose the simpler approach that satisfies performance requirements.

---

# 63. KIE Session Lifecycle

Do not create a new complete Drools container from scratch for every ticker.

Recommended:

```text
Application Startup
        ↓
Load/compile active rules
        ↓
Create KieContainer
        ↓
Reuse container
        ↓
Create execution session per evaluation/batch
```

Rules should be cached.

When an active rule changes:

```text
UI
 ↓
Rule API
 ↓
Validate
 ↓
Persist new version
 ↓
Activate
 ↓
Invalidate/rebuild rule cache
```

The application should avoid interrupting currently running evaluations.

---

# 64. Rule Cache

Recommended cache layers:

```text
Active Profile
     ↓
Active Rules
     ↓
Compiled Rule Set
```

Cache key can include:

```text
profileCode
ruleSetVersion
```

When a rule is changed:

```text
old cache
    ↓
invalidate
    ↓
load new active rule set
```

---

# 65. REST APIs

Baseline APIs:

```text
GET    /api/decision/indicators
POST   /api/decision/indicators
PUT    /api/decision/indicators/{id}
PATCH  /api/decision/indicators/{id}/enable
```

Rules:

```text
GET    /api/decision/rules
GET    /api/decision/rules/{id}
POST   /api/decision/rules
PUT    /api/decision/rules/{id}
POST   /api/decision/rules/{id}/validate
POST   /api/decision/rules/{id}/activate
POST   /api/decision/rules/{id}/disable
GET    /api/decision/rules/{id}/versions
```

Profiles:

```text
GET    /api/decision/profiles
POST   /api/decision/profiles
PUT    /api/decision/profiles/{id}
```

Execution:

```text
POST   /api/decision/evaluate
POST   /api/decision/test-rule
```

Pipelines (only needed for chained/funnel screening — §65b):

```text
GET    /api/decision/pipelines
POST   /api/decision/pipelines
POST   /api/decision/pipelines/{code}/execute
```

See §65a for the `evaluate` request shape — specifically how to target it at
specific subjects (tickers) instead of a whole universe. See §65b for
chaining multiple profiles into a narrowing funnel (e.g. high volume, then
high RSI within those results).

---

# 65a. Execution Scope / Target Selection

This is the missing piece that ties the single-subject flow (§18) and the
batch flow (§19a) together behind one endpoint: **the caller declares what
to run the rule against via a `scope` object**, and the backend decides
whether that resolves to one `evaluate()` call or a batch `evaluateBatch()`
call. Nothing about the rule definition changes based on scope — scope is
purely "who do I run this against," never part of the rule itself.

```json
POST /api/decision/evaluate
{
  "profileCode": "RISK_ADJUSTMENT",
  "asOfDate": "2026-09-06",
  "scope": {
    "type": "EXPLICIT",
    "subjectType": "TICKER",
    "subjectIds": ["RELIANCE", "TCS", "INFY"]
  }
}
```

`scope.type` options:

```text
EXPLICIT   -- run only against the listed subjectIds (this is "run on
              specific tickers" — pass exactly the ones you want, one or many)
ALL        -- run against every enabled subject of subjectType known to
              the application (e.g. the full tradable universe)
WATCHLIST  -- run against a saved list the user already has, referenced by id
              (only applicable if the host application has a watchlist/
              universe concept; omit this option otherwise)
QUERY      -- run against subjects matched by an existing application query/
              filter (e.g. "all NIFTY 500 constituents"), referenced by a
              query id or saved filter id rather than re-implemented here
```

Backend resolution:

```text
scope.type = EXPLICIT with 1 subjectId   → call evaluate() (§18)
scope.type = EXPLICIT with N subjectIds  → call evaluateBatch() (§19a)
                                            with those N subjects only
scope.type = ALL / WATCHLIST / QUERY     → resolve subjectIds first via the
                                            relevant existing service, then
                                            call evaluateBatch()
```

Response shape scales the same way regardless of scope size:

```json
{
  "profileCode": "RISK_ADJUSTMENT",
  "asOfDate": "2026-09-06",
  "results": [
    {
      "subjectType": "TICKER",
      "subjectId": "RELIANCE",
      "outputs": { "RISK_SCORE": 95 },
      "rulesFired": ["RSI Percentile Risk", "PVPP Rule", "ATR Rule"]
    },
    {
      "subjectType": "TICKER",
      "subjectId": "TCS",
      "outputs": { "RISK_SCORE": 88 },
      "rulesFired": ["RSI Percentile Risk"]
    }
  ]
}
```

For a `FILTER`-mode profile (the screener case), the same response shape
applies, but the service pre-filters `results` down to subjects whose
declared boolean output is `true` before returning — so "get me tickers
with high volume" is:

```json
POST /api/decision/evaluate
{
  "profileCode": "HIGH_VOLUME_SCREEN",
  "asOfDate": "2026-09-06",
  "scope": { "type": "ALL", "subjectType": "TICKER" }
}
```

and the response's `results` array *is* the answer — every ticker in it
already passed the rule; nothing further needs to be computed by the
caller.

If you specifically want a small explicit set instead of the whole market —
"just check RELIANCE, TCS, and INFY against my risk rules" — that's the
`EXPLICIT` scope example above: list exactly those `subjectIds` and only
those three get evaluated, regardless of what profile or evaluation_mode is
in use.

---

# 65b. Rule Pipelines / Chained Screening (Funnel Narrowing)

This directly answers "screen for high volume first, then run RSI > 80 only
on the ones that passed." Two ways to get there — one needs no new
building blocks at all, the other is a small convenience layer worth adding
because this pattern (narrow, then narrow again) is common enough to name.

## Option A — compose two calls yourself, zero new features

Every `evaluate` response already returns the matching `subjectIds`
(§65a). Feed stage 1's output straight into stage 2's `scope`:

```json
// Stage 1 — 100 tickers in, 10 pass
POST /api/decision/evaluate
{
  "profileCode": "HIGH_VOLUME_SCREEN",
  "asOfDate": "2026-09-06",
  "scope": { "type": "ALL", "subjectType": "TICKER" }
}
→ results: 10 tickers, e.g. ["TATASTEEL", "SAIL", ... ]
```

```json
// Stage 2 — only those 10 go in, some smaller number pass RSI > 80
POST /api/decision/evaluate
{
  "profileCode": "HIGH_RSI_SCREEN",
  "asOfDate": "2026-09-06",
  "scope": {
    "type": "EXPLICIT",
    "subjectType": "TICKER",
    "subjectIds": ["TATASTEEL", "SAIL", "..."]
  }
}
→ final results: tickers with RVOL high AND RSI > 80
```

This works today with nothing added to the schema — it's just two profiles
(`HIGH_VOLUME_SCREEN`, `HIGH_RSI_SCREEN`), each a plain `FILTER`-mode
profile exactly like the one built in §87's second scenario, called back
to back by the client.

## Option B — a saved, reusable pipeline (recommended if you'll rerun this funnel)

If this volume→RSI funnel is something you'll run repeatedly (daily scan,
a saved screener), it's worth persisting the chain itself instead of
re-wiring two calls every time.

```sql
CREATE TABLE decision_pipeline (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id BINARY(16) NOT NULL,
    owner_id BIGINT NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    subject_type VARCHAR(50) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_decision_pipeline_public_id (public_id),
    UNIQUE KEY uk_decision_pipeline_owner_code (owner_id, code)
);

CREATE TABLE decision_pipeline_stage (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pipeline_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    profile_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_pipeline_stage_seq (pipeline_id, sequence_no),

    CONSTRAINT fk_pipeline_stage_pipeline
        FOREIGN KEY (pipeline_id) REFERENCES decision_pipeline(id),
    CONSTRAINT fk_pipeline_stage_profile
        FOREIGN KEY (profile_id) REFERENCES decision_profile(id)
);
```

Each stage's `profile_id` must reference a `FILTER`-mode profile (§42) —
a stage's job is to narrow the set, so its output has to resolve to a
boolean "did this subject pass" per subject. (`ACCUMULATE`/`CLASSIFY`
stages can still participate in a funnel, but only once their score/tag is
exposed as a derived indicator for the next stage's conditions to read —
that's a separate future extension, not needed for a volume→RSI chain
where both stages are naturally FILTER.)

Execution, reusing everything already defined — no new engine logic, just
a loop over `evaluateBatch` (§19a) that narrows `subjects` each iteration:

```text
subjects = resolve(scope)                     // e.g. ALL tickers → 100

for stage in pipeline.stages (by sequence_no):
    stageResults = evaluateBatch(subjects, stage.profile)
    matched      = stageResults where declared boolean output == true
    record { profileCode, inputCount: subjects.size, matchedCount: matched.size }
    subjects = matched.subjectIds                // this narrows the next stage

return finalSubjects = subjects, plus the per-stage funnel breakdown
```

API:

```json
POST /api/decision/pipelines
{
  "code": "VOLUME_THEN_RSI",
  "name": "High Volume → High RSI",
  "subjectType": "TICKER",
  "stages": [
    { "profileCode": "HIGH_VOLUME_SCREEN" },
    { "profileCode": "HIGH_RSI_SCREEN" }
  ]
}
```

```json
POST /api/decision/pipelines/VOLUME_THEN_RSI/execute
{
  "asOfDate": "2026-09-06",
  "scope": { "type": "ALL", "subjectType": "TICKER" }
}
```

Response — shows the funnel narrowing exactly as described (100 → 10 → N),
not just the final answer:

```json
{
  "pipelineCode": "VOLUME_THEN_RSI",
  "stages": [
    { "profileCode": "HIGH_VOLUME_SCREEN", "inputCount": 100, "matchedCount": 10 },
    { "profileCode": "HIGH_RSI_SCREEN",    "inputCount": 10,  "matchedCount": 3  }
  ],
  "finalResult": {
    "subjectIds": ["TATASTEEL", "SAIL", "JINDALSTEL"],
    "count": 3
  }
}
```

Same registry, same profiles, same `FILTER` mode from §42 — a pipeline is
just an ordered list of profile references plus a loop that feeds one
stage's matches into the next stage's scope. No new indicator, no new
action type, no new evaluation mode required.

---

# 65c. Wrapping Existing Methods That Take Configurable Parameters

`ConsistentVolumeProvider` was already built out fully in §5 as one of the
two canonical `IndicatorProvider` examples — this section covers the one
thing not yet shown there: **where a specific rule's chosen parameter
values (which ones are fixed, which are computed) actually get stored**,
and how they're resolved at execution time. No separate interface, adapter
type, or rule type is needed beyond what §5 already defines — an indicator
backed by an existing multi-parameter method is still just an
`IndicatorProvider`, used in an ordinary condition.

## Storing a rule's chosen parameter values

`getParameterSpecs()` (§5) declares each parameter's type and *default*
source. A specific rule condition can keep those defaults or override
them — this is the "define at rule creation what's hardcoded vs dynamic"
you described. Rather than a separate table, this is a single JSON column
on the condition itself, since a parameter set only ever makes sense in
the context of one specific use of one specific indicator:

```sql
ALTER TABLE decision_rule_condition
    ADD COLUMN indicator_params_json JSON;
```

```json
{
  "fromDate":                 { "source": "EXPRESSION", "value": "today().minusMonths(18)" },
  "toDate":                   { "source": "EXPRESSION", "value": "today()" },
  "inputBaselineWindow":      { "source": "CONSTANT",   "value": 20 },
  "baselineLowPercentile":    { "source": "CONSTANT",   "value": 10 },
  "baselineHighPercentile":   { "source": "CONSTANT",   "value": 90 },
  "baseRvolPercentileWindow": { "source": "CONSTANT",   "value": 60 },
  "rvolThresholdPercentile":  { "source": "CONSTANT",   "value": 75 },
  "consistencyWindow":        { "source": "CONSTANT",   "value": 10 },
  "requiredScore":            { "source": "CONSTANT",   "value": 7 }
}
```

If a condition omits an entry, the indicator's own `getParameterSpecs()`
default is used — most rules will only ever override a couple of values
(e.g. a stricter `requiredScore`) and leave the rest at the provider's
defaults.

Example configuration a user would create in the UI — an ordinary
condition, nothing special about the rule as a whole:

```text
Rule: "18mo Consistent Volume Screen"
Condition: CONSISTENT_VOLUME == true

Parameters shown to user, pre-filled from the provider's declared
defaults, editable:
  fromDate                  [EXPRESSION]  today().minusMonths(18)
  toDate                    [EXPRESSION]  today()
  inputBaselineWindow       [CONSTANT]    20
  baselineLowPercentile     [CONSTANT]    10
  baselineHighPercentile    [CONSTANT]    90
  baseRvolPercentileWindow  [CONSTANT]    60
  rvolThresholdPercentile   [CONSTANT]    75
  consistencyWindow         [CONSTANT]    10
  requiredScore             [CONSTANT]    7
```

The user only ever edits values — they never see or need to know that
`fromDate` is computed dynamically while `inputBaselineWindow` is fixed;
the provider's declared `defaultSource` already tells the UI which input
to render as an expression box versus a plain number field.

## Execution

Required-indicator discovery (§19) already determines which indicators a
profile's active rules need; this is unchanged for parameterized
indicators — resolving their parameters just happens once, before
`resolveValues` (§5) is called:

```text
1. For a condition referencing a parameterized indicator, read
   indicator_params_json (falling back to the provider's own
   getParameterSpecs() defaults for anything not overridden)
2. For each parameter:
     source = CONSTANT   → use the stored value, cast to the declared ParamType
     source = EXPRESSION → evaluate the stored expression via SpEL against
                            an EvaluationContext exposing helper functions
                            (today(), daysAgo(n), monthsAgo(n), plus the
                            current asOfDate/subjectId if relevant)
3. Call provider.resolveValues(subjects, resolvedParams) once for the
   whole batch (§19a) — not once per subject
4. Each subject's resolved value is inserted into its SubjectContext (§6)
   exactly like any other indicator, and evaluated in the condition
   (CONSISTENT_VOLUME == true) exactly like RSI or VCP
```

`today()`, `daysAgo(n)`, `monthsAgo(n)` are simple custom SpEL functions
registered once in the evaluation context — this is the whole answer to
"the date should be 1.5 years back and today, computed fresh every run":
the expression `today().minusMonths(18)` is stored once and re-evaluated
at execution time, never baked into a stored date value.

Because this is an ordinary indicator, it composes with everything already
covered: it can appear alongside other conditions in a normal rule
(`CONSISTENT_VOLUME == true AND RVOL > 3`), or be the sole condition of a
`FILTER`-mode profile used as a pipeline stage (§65b) — no special casing
anywhere in the engine for "this indicator happens to be backed by a
batch method with configurable parameters."

## Is there a library for this?

Not a drop-in one for the whole thing, but two pieces of it are standard:

- **SpEL** (Spring Expression Language, already on the classpath in any
  Spring app) is exactly the right tool for the `EXPRESSION` parameter
  type — it supports method calls, arithmetic, and custom registered
  functions, so `today().minusMonths(18)` is a real, evaluable expression,
  not something you'd hand-parse yourself.
- **The overall pattern** (declare an input's source as constant-or-
  expression, store that per configured use, resolve and invoke at run
  time) is the same one BPM engines like **Camunda**/**Flowable** use for
  Java Delegates with input/output parameter mappings. If you ever outgrow
  this — e.g. you want branching, retries, or human approval steps around
  these calls — that family of tools is worth a look. For just "call this
  method with a mix of fixed and computed inputs," the `IndicatorProvider`
  + `ParamSpec` + SpEL approach above is simpler and adds no new runtime
  dependency beyond what a typical Spring app already has.

---

# 66. Example Create Rule API

Request:

```http
POST /api/decision/rules
```

```json
{
  "profileCode": "MOMENTUM",
  "name": "RSI Percentile Risk Adjustment",
  "ruleType": "RISK",
  "priority": 10,
  "conditions": [
    {
      "indicator": "RSI_PERCENTILE",
      "operator": ">=",
      "value": 0
    }
  ],
  "actions": [
    {
      "target": "RISK_SCORE",
      "type": "DECREASE",
      "expression": {
        "operator": "SUBTRACT",
        "left": 100,
        "right": {
          "indicator": "RSI_PERCENTILE"
        }
      }
    }
  ]
}
```

Response:

```json
{
  "ruleId": 42,
  "version": 1,
  "status": "DRAFT"
}
```

---

# 67. Example Rule Validation

When validating:

```text
POST /api/decision/rules/42/validate
```

the backend checks:

```text
✓ RSI_PERCENTILE exists
✓ RSI_PERCENTILE enabled
✓ Numeric type
✓ >= valid
✓ Formula valid
✓ Referenced indicator exists
✓ Formula has no unsupported operation
✓ No circular dependency
✓ Action target valid
```

Then:

```text
status = VALIDATED
```

---

# 68. Example Rule Test

Request:

```json
{
  "ticker": "RELIANCE",
  "date": "2026-09-06",
  "profileCode": "MOMENTUM"
}
```

Runtime:

```text
RSI_PERCENTILE = 90
PVPP = 0.0032
ATR = 4.2
```

Result:

```json
{
  "initialRisk": 100,
  "finalRisk": 95,
  "rulesFired": [
    {
      "rule": "RSI Percentile Risk Adjustment",
      "calculation": "100 - 90",
      "adjustment": -10
    },
    {
      "rule": "Strong PVPP",
      "adjustment": -15
    },
    {
      "rule": "High ATR",
      "adjustment": 20
    }
  ]
}
```

---

# 69. Angular UI Data Flow

```text
Angular
   │
   ├── GET /indicators
   │       ↓
   │   Indicator Registry
   │
   ├── User creates rule
   │       ↓
   │   Rule Builder
   │
   ├── POST /rules
   │       ↓
   │   Backend validation
   │
   ├── POST /rules/{id}/validate
   │
   ├── POST /rules/{id}/activate
   │
   └── POST /evaluate
           ↓
       Decision Engine
```

The UI should never need to understand Drools internals.

---

# 70. UI Indicator Dropdown

The UI should call:

```text
GET /api/decision/indicators
```

and display:

```text
Indicator
──────────────
RSI
RSI Percentile
ATR
ATR Percentile
PVPP
RVOL
MK Tau
MK Slope
VCP
Sortino
Delivery %
```

Only:

```text
enabled = true
```

indicators should be selectable.

---

# 71. UI Formula Builder

The formula builder should expose:

```text
CONSTANT
INDICATOR
+
-
*
/
ABS
MIN
MAX
CLAMP
```

Example:

```text
[ 100 ]
   -
[ RSI Percentile ]
```

The UI produces:

```json
{
  "operator": "SUBTRACT",
  "left": 100,
  "right": {
    "indicator": "RSI_PERCENTILE"
  }
}
```

---

# 72. Rule Execution Example

Assume:

```text
Initial Risk = 100

Rule 1:
RSI Percentile = 90
→ -(100 - RSI Percentile)

Rule 2:
PVPP > 0.002
→ -15

Rule 3:
ATR > 4
→ +20
```

Runtime:

```text
Start
Risk = 100

Rule 1:
100 - 90 = 10
Risk = 90

Rule 2:
Risk = 90 - 15
Risk = 75

Rule 3:
Risk = 75 + 20
Risk = 95

Clamp:
95 remains 95

Final Risk = 95
```

The risk score exists only for the current decision evaluation.

---

# 73. Rule Ordering

Rules must not depend accidentally on database retrieval order.

Use:

```text
priority
```

for deterministic execution.

Example:

```text
Priority 100 → emergency/rejection rules
Priority 50  → major risk adjustments
Priority 10  → normal adjustments
```

For additive risk adjustments, all applicable rules may execute.

For mutually exclusive decisions:

```text
BUY
SELL
HOLD
REJECT
```

use explicit conflict resolution.

---

# 74. Risk Calculation Policy

A configurable policy should define:

```text
initialRisk
minimumRisk
maximumRisk
```

Example:

```text
initialRisk = 100
minimumRisk = 0
maximumRisk = 100
```

Before returning:

```java
riskState.clamp();
```

This prevents rule combinations from producing invalid values.

---

# 75. Transaction Boundaries

Rule administration:

```text
Create rule
Update rule
Validate
Activate
Disable
```

should be transactional.

Example:

```java
@Transactional
public RuleVersion activate(Long ruleId) {
    ...
}
```

Activation should atomically:

```text
1. Validate rule
2. Create/mark version active
3. Update current version
4. Update rule status
5. Publish cache invalidation event
```

The currently executing rule set should remain stable for an individual decision evaluation.

---

# 76. Concurrency

For large-scale evaluation:

```text
Ticker A → Session A
Ticker B → Session B
Ticker C → Session C
```

Do not share mutable `DecisionState` between tickers.

Each evaluation must have its own:

```text
SubjectContext
DecisionState
DecisionContext
```

Drools sessions should not be treated as globally shared mutable state unless the selected Drools version explicitly supports the intended usage pattern.

---

# 77. Batch Processing

For processing thousands of tickers:

```text
Load active rule set once
        ↓
Discover required indicators once
        ↓
Bulk-load indicator data where possible
        ↓
Evaluate ticker contexts
        ↓
Return decisions
```

Avoid:

```text
Ticker
 ↓
Load rule DB
 ↓
Load indicator registry
 ↓
Compile Drools
 ↓
Evaluate
```

for every ticker.

That would be unnecessarily expensive.

---

# 78. Logging

Every evaluation should support structured logging.

Example:

```text
ticker=RELIANCE
profile=MOMENTUM
initialRisk=100
finalRisk=95
rulesFired=3
executionTimeMs=4
```

Debug mode can additionally log:

```text
ruleId
ruleVersion
indicator values
condition result
formula result
risk adjustment
```

Avoid excessive INFO-level logging for thousands of ticker evaluations.

---

# 79. Error Handling

Examples:

```text
IndicatorNotRegisteredException
IndicatorDataUnavailableException
RuleValidationException
UnsupportedOperatorException
InvalidExpressionException
RuleCompilationException
DecisionExecutionException
```

The API should return meaningful error messages.

Example:

```json
{
  "code": "INDICATOR_NOT_AVAILABLE",
  "message": "Indicator RSI_PERCENTILE is not enabled.",
  "field": "conditions[0].indicator"
}
```

---

# 80. Rule Validation Before Activation

A rule must pass:

```text
Structural validation
        ↓
Indicator validation
        ↓
Type validation
        ↓
Expression validation
        ↓
Execution compilation/test
        ↓
Activation
```

Never activate a rule that has not passed all required checks.

---

# 81. Security

The UI rule builder must not allow arbitrary code.

Never allow:

```text
Java code
Java reflection
SQL
Shell commands
Arbitrary JavaScript
Arbitrary DRL from normal users
```

Only allow operators/functions explicitly supported by the expression engine.

User input should be treated as data.

---

# 82. Testing Strategy

## Unit Tests

Test:

```text
FormulaEvaluator
ConditionEvaluator
DecisionState
RuleValidator
IndicatorRegistry
```

Example:

```text
RSI percentile = 90
formula = 100 - RSI percentile
expected = 10
```

## Integration Tests

Test:

```text
UI JSON
 ↓
Rule API
 ↓
Database
 ↓
Rule compilation
 ↓
Drools
 ↓
DecisionResult
```

## Regression Tests

Every production rule set should have representative test cases.

---

# 83. Rule Simulation

Before activating a rule, provide:

```text
TEST RULE
```

The backend should execute the rule against historical data.

Example:

```text
Last 100 trading days

Before rule:
Average risk = 63

After rule:
Average risk = 54

Difference:
-9
```

This is useful before allowing a rule to affect live decisions.

---

# 84. Backtesting

The decision engine must not directly depend on live database calls.

Use:

```text
IndicatorProvider
```

as the abstraction.

Then:

```text
LiveIndicatorProvider
HistoricalIndicatorProvider
```

can both implement the same interface.

Therefore:

```text
Live:
Market Data → IndicatorProvider → SubjectContext

Backtest:
Historical Data → IndicatorProvider → SubjectContext
```

The same rules execute in both cases.

---

# 85. Migration Strategy

Database changes should use the project's existing migration mechanism.

If the project uses Flyway:

```text
Vxxx__create_decision_profile.sql
Vxxx__create_decision_indicator.sql
Vxxx__create_decision_output_variable.sql
Vxxx__create_decision_rule.sql
Vxxx__create_decision_rule_version.sql
Vxxx__create_decision_rule_condition.sql
Vxxx__create_decision_rule_action.sql
Vxxx__create_decision_pipeline.sql          -- only if chained screening (§65b) is needed
Vxxx__create_decision_pipeline_stage.sql    -- only if chained screening (§65b) is needed
```

If Liquibase is already used, use Liquibase instead.

Do not introduce a second migration framework.

---

# 86. Recommended Implementation Sequence

## Phase 1 — Foundation

```text
1. Add compatible Drools/KIE dependencies
2. Create database tables (including decision_output_variable)
3. Create Indicator Registry
4. Create Output Variable Registry
5. Create IndicatorProvider interface
6. Create SubjectContext
7. Create generic DecisionState (map-based, not a single score)
8. Create basic RuleDefinition model with evaluation_mode
```

## Phase 2 — Backend Rule Management

```text
9. Rule CRUD APIs (owner-scoped)
10. Rule validation (conditions vs indicators, actions vs output variables)
11. Rule versioning
12. Rule activation/deactivation
13. Indicator APIs
14. Decision profiles (with evaluation_mode + declared outputs)
```

## Phase 3 — Execution

```text
15. Formula evaluator
16. Condition evaluator
17. Drools integration
18. Single-subject rule execution
19. Batch/universe rule execution (§19a) — needed for screener-style profiles
20. DecisionState modification
21. DecisionResult
22. Explanation output
```

## Phase 4 — UI

```text
23. Indicator management screen
24. Output variable management screen
25. Profile builder (evaluation_mode selector)
26. Rule list
27. Rule builder
28. Formula builder
29. Rule validation
30. Rule activation
31. Rule test/preview
```

## Phase 5 — Production Hardening

```text
32. Rule caching
33. Cache invalidation
34. Structured logging
35. Metrics
36. Concurrency testing
37. Batch optimization
38. Backtesting
39. Rule simulation
```

---

# 87. Definition of Done for Baseline Engine

The baseline implementation is complete when the following scenario works end-to-end.

### Step 1

Indicator registry contains:

```text
RSI_PERCENTILE
PVPP
ATR
```

### Step 2

User creates from UI:

```text
Rule 1:
RSI_PERCENTILE >= 0
→ DECREASE RISK BY (100 - RSI_PERCENTILE)

Rule 2:
PVPP > 0.002
→ DECREASE RISK BY 15

Rule 3:
ATR > 4
→ INCREASE RISK BY 20
```

### Step 3

Rules are saved as versions.

### Step 4

Rules are validated.

### Step 5

Rules are activated.

### Step 6

Runtime receives:

```text
RSI_PERCENTILE = 90
PVPP = 0.0032
ATR = 4.2
```

### Step 7

Engine starts:

```text
Risk = 100
```

### Step 8

Engine calculates:

```text
RSI:
100 - 90 = 10
Risk = 90

PVPP:
Risk = 75

ATR:
Risk = 95
```

### Step 9

Engine returns:

```text
Final Risk = 95
```

plus:

```text
Rules Fired
Calculations
Adjustments
Rule Versions
Execution Time
```

No risk score needs to exist in the indicator tables.

## Second scenario — proves the engine is generic, not just done for risk

The baseline is not actually complete until this second scenario also
works, unmodified, against the same tables and the same execution service:

### Step 1

Indicator registry already contains `RVOL` (no new indicator needed).

### Step 2

User creates a new profile from the UI:

```text
Profile: HIGH_VOLUME_SCREEN
evaluation_mode = FILTER
subject_type = TICKER
output: MATCHED (BOOLEAN, initial = false)
```

### Step 3

User creates from UI:

```text
Rule 1:
RVOL > 3
→ SET MATCHED = true
```

### Step 4

Rule is saved as a version, validated, and activated — same pipeline as
the risk rules above, no code changes.

### Step 5

Runtime calls the batch entry point (§19a):

```text
evaluateBatch(all_tickers_today, HIGH_VOLUME_SCREEN)
```

### Step 6

Engine returns the subset of tickers where `MATCHED = true` — this is the
literal answer to "get me tickers with high volume," produced without a
`RiskState`, a `ticker`/`execution_date`-shaped audit row, or any
risk-specific code path being touched.

If this scenario requires touching Java code beyond writing a new
`RvolProvider` (which already exists in this example) or adding rows to
`decision_profile` / `decision_output_variable` / `decision_rule*`, the
engine is not yet generic — it is still a risk calculator with an
extensibility story.

---

# 88. Future Extension: Risk Score vs Risk Allocation

The baseline should keep these concepts separate.

```text
Risk Score
     ↓
Risk Level / Risk Factor
     ↓
Position Size
```

For example:

```text
Risk Score = 35
        ↓
Position Multiplier = 0.65
        ↓
Position Size = Base Position × 0.65
```

Do not hard-code this conversion into the indicator rules.

This allows the same risk score to later be used by different position-sizing strategies.

---

# 89. Multi-Dimensional Decision State (Now Baseline, Not Future)

Earlier drafts of this architecture treated this as a future extension.
It is folded into the baseline as of §7/§15/§43a: `DecisionState` is a
generic named map from the start, not a single `score` field that gets
generalized later. The same profile-scoped registry can back any of:

```text
RiskScore
QualityScore
MomentumScore
TrendScore
ConfidenceScore
PositionMultiplier
MatchedFlag        -- boolean, used by screener/FILTER profiles
QualityTag         -- string, used by CLASSIFY profiles
```

Example, one profile maintaining several outputs at once:

```text
RiskScore          = 42
QualityScore       = 81
MomentumScore      = 76
TrendScore         = 84
Confidence         = 88
PositionMultiplier = 0.72
```

There is no assumption anywhere in the engine that the only runtime
variable is `RiskScore` — each profile declares its own output variables
in `decision_output_variable` (§43a), and `DecisionState` (§15) simply
holds whatever that profile declared.

---

# 90. Final Technical Architecture

```text
                         ┌───────────────────────┐
                         │      Angular UI       │
                         │                       │
                         │ Indicator Registry    │
                         │ Rule Builder          │
                         │ Formula Builder       │
                         │ Rule Testing          │
                         └───────────┬───────────┘
                                     │ REST
                                     ▼
                         ┌───────────────────────┐
                         │ Spring Boot           │
                         │                       │
                         │ Controllers           │
                         │ Rule Services         │
                         │ Validation            │
                         │ Compilation           │
                         └───────────┬───────────┘
                                     │
                ┌────────────────────┼─────────────────────┐
                │                    │                     │
                ▼                    ▼                     ▼
       ┌────────────────┐   ┌────────────────┐   ┌─────────────────┐
       │ Indicator      │   │ Rule Repository│   │ Rule Cache      │
       │ Registry       │   │                │   │ / KIE Container │
       └───────┬────────┘   └────────────────┘   └────────┬────────┘
               │                                          │
               ▼                                          ▼
       ┌────────────────┐                         ┌────────────────┐
       │ Indicator      │                         │ Drools Engine  │
       │ Providers      │                         └───────┬────────┘
       └───────┬────────┘                                 │
               │                                          │
               ▼                                          │
       ┌────────────────┐                                  │
       │ SubjectContext  │──────────────────────────────────┘
       └────────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │ DecisionState    │
                  │ initial=100  │
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │ DecisionState│
                  │              │
                  │ Risk         │
                  │ Signal       │
                  │ Confidence   │
                  │ Position     │
                  └──────┬───────┘
                         │
                         ▼
                  ┌──────────────┐
                  │ DecisionResult│
                  │              │
                  │ Final values │
                  │ Rules fired  │
                  │ Explanation  │
                  └──────────────┘
```

The resulting system is a **configuration-driven, extensible decision engine** where adding an indicator and its provider makes that indicator available to the UI, while rule creation and calculation remain generic.