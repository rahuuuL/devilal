# Mann-Kendall Trend Calculator - Technical Requirements

# Objective

Implement a reusable Java component that performs Mann-Kendall trend analysis on a numeric time series and returns statistical metrics used for trend detection and persistence.

The component will be used by batch processing, historical analysis generation, and future analytics modules.

---

# Functional Requirements

## Input

The calculator shall accept:

```java
List<Double> values
```

Requirements:

* Input cannot be null.
* Input must contain at least 3 observations.
* Input values must be finite numeric values.
* Observations must be ordered chronologically from oldest to newest.

Example:

```java
List<Double> prices =
        List.of(
            100.0,
            101.5,
            103.2,
            104.8
        );
```

---

# Output

The calculator shall return:

```java
public class MannKendallCalcResult {

    private String trend;

    private boolean h;

    private double p;

    private double z;

    private double tau;

    private long s;

    private double varS;

    private double slope;

    private double intercept;
}
```

---

# Calculator Interface

```java
public interface MannKendallCalculator {

    MannKendallResult calculate(
            List<Double> values
    );
}
```

---

# Statistical Calculations

The calculator shall compute:

* Mann-Kendall S statistic
* Variance of S
* Z score
* Kendall Tau
* P value
* Significance flag
* Trend classification
* Sen Slope
* Intercept

---

# S Statistic

For all pairs:

```text
i < j
```

Compare:

```text
values[j] - values[i]
```

Rules:

```text
positive difference -> +1
negative difference -> -1
zero difference     ->  0
```

S statistic:

```text
S = Σ sign(values[j] - values[i])
```

---

# Tie Handling

The implementation shall detect duplicate values.

Example:

```text
100
100
100
105
110
110
```

Tie groups:

```text
100 -> size 3
110 -> size 2
```

Tie group sizes shall be incorporated into variance calculations.

---

# Variance Calculation

The calculator shall compute variance using tie-adjusted variance formulas.

Inputs:

```text
n
tie groups
```

Output:

```text
Var(S)
```

---

# Z Score

The calculator shall compute:

```text
Z
```

using standard Mann-Kendall formulas and continuity correction.

Output:

```text
positive value
negative value
or zero
```

---

# Kendall Tau

The calculator shall compute:

```text
Tau
```

representing trend strength.

Expected range:

```text
-1.0 <= Tau <= 1.0
```

Interpretation:

```text
Negative values -> downward trend

Positive values -> upward trend

Near zero -> weak trend
```

---

# P Value

The calculator shall compute:

```text
p
```

using the standard normal distribution.

Expected range:

```text
0.0 <= p <= 1.0
```

---

# Significance Flag

The calculator shall determine statistical significance.

Configuration:

```text
alpha = 0.05
```

Rules:

```text
p < alpha  -> true

otherwise  -> false
```

Output:

```java
boolean h;
```

---

# Trend Classification

Trend shall be determined using:

```text
h
z
```

Possible values:

```text
increasing
decreasing
no trend
```

Rules:

```text
h = false
    => no trend

h = true and z > 0
    => increasing

h = true and z < 0
    => decreasing
```

---

# Sen Slope

The calculator shall compute Sen Slope.

For every pair:

```text
i < j
```

Calculate:

```text
(values[j] - values[i])
/
(j - i)
```

Generate all slopes.

Sort all slopes.

Sen Slope:

```text
Median of all pairwise slopes
```

Output:

```java
double slope;
```

---

# Intercept

The calculator shall compute:

```text
Intercept
```

using:

```text
Median(Y)
-
(
 SenSlope
 *
 Median(TimeIndex)
)
```

Output:

```java
double intercept;
```

---

# Result Object Population

The returned result shall contain:

```text
trend
h
p
z
tau
s
varS
slope
intercept
```

for every successful calculation.

---

# Error Handling

The calculator shall throw:

```java
IllegalArgumentException
```

when:

* Input is null
* Input contains fewer than 3 observations
* Input contains invalid numeric values

Error messages should clearly identify the validation failure.

---

# Performance Requirements

Expected workload:

```text
Thousands of tickers
Multiple rolling windows
Batch execution
```

Requirements:

* Avoid unnecessary object creation.
* Minimize repeated sorting operations.
* Use primitive calculations where practical.
* Ensure thread-safe usage.
* Calculator implementation must be stateless.

---

# Reusability Requirements

The calculator shall be reusable by:

* Historical MK generation
* Daily trend generation
* Regime detection modules
* Alerting systems
* Future analytics modules

The implementation shall contain no database access.

The implementation shall contain no HTTP calls.

The implementation shall contain no framework-specific dependencies.

The calculator shall operate solely on in-memory numeric data.

---

# Testing Requirements

Unit tests shall verify:

* Increasing series
* Decreasing series
* Flat series
* Series with ties
* Small datasets
* Large datasets
* Significant trends
* Non-significant trends

Validation tests shall verify:

* Null input
* Empty input
* Less than 3 observations
* Invalid numeric values

Coverage should include all calculation branches and edge cases.

---

# Deliverables

Required classes:

```text
MannKendallCalculator
MannKendallCalculatorImpl
MannKendallCalcResult
StatisticsUtils
```

Required test classes:

```text
MannKendallCalculatorTest
StatisticsUtilsTest
```

The final implementation must be a standalone, framework-independent Java component capable of performing Mann-Kendall trend analysis on an ordered numeric time series.
