# Mann-Kendall History Performance Optimization Plan (Phase 2)

## Current State

### Performance Metrics

| Operation          | Time      |
| ------------------ | --------- |
| Days=10            | ~109 ms   |
| Days=20            | ~71 ms    |
| Days=30            | ~69 ms    |
| Days=40            | ~181 ms   |
| Days=50            | ~120 ms   |
| Days=60            | ~135 ms   |
| Days=180           | ~906 ms   |
| UPSERT 13,166 rows | ~2,576 ms |
| Total Runtime      | ~5-6 sec  |

### Improvements Already Implemented

#### Removed Delete + Insert Strategy

Old Flow:

```text
DELETE existing rows
INSERT new rows
```

Problems:

* Double database work
* Large transaction overhead
* Lock contention

Replaced With:

```sql
INSERT ...
ON DUPLICATE KEY UPDATE ...
```

Result:

```text
26 sec → 12 sec
```

---

#### Replaced JDBC Batch UPSERT

Old Flow:

```java
batchUpdate(
    "INSERT ... VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE ..."
)
```

This executed one statement per row.

Result:

```text
~10 sec UPSERT
```

---

#### Implemented Multi-Row UPSERT

Current Flow:

```sql
INSERT INTO mk_result_history
(...)
VALUES
(...),
(...),
(...),
...
ON DUPLICATE KEY UPDATE
...
```

Chunked in batches.

Result:

```text
UPSERT:
9.8 sec → 2.5 sec
```

---

## Remaining Optimization Opportunities

---

# Optimization 1: Parallel Window Processing

## Current Problem

Each window executes sequentially.

Current Flow:

```java
for (Integer days : configDays) {
    processWindow(days);
}
```

Execution Order:

```text
10
20
30
40
50
60
180
```

Every window waits for the previous window to finish.

---

## Proposed Solution

Execute windows concurrently.

Example:

```java
List<CompletableFuture<List<MannKendallResult>>> futures =
        configDays.stream()
                .map(days ->
                        CompletableFuture.supplyAsync(
                                () -> processWindow(days),
                                executorService))
                .toList();

CompletableFuture.allOf(
        futures.toArray(new CompletableFuture[0]))
        .join();
```

---

## Recommended Executor

```java
@Bean
public ExecutorService mkWindowExecutor() {
    return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());
}
```

---

## Expected Benefit

Current:

```text
~1.5-2.0 sec
```

Expected:

```text
~500-800 ms
```

Savings:

```text
700-1200 ms
```

---

# Optimization 2: Fetch Market Data Once

## Current Problem

Database is queried separately for every window.

Current Pattern:

```java
fetchData(10)
fetchData(20)
fetchData(30)
fetchData(40)
fetchData(50)
fetchData(60)
fetchData(180)
```

Observed:

```text
44 ms
43 ms
67 ms
72 ms
73 ms
224 ms
...
```

Seven separate database reads.

---

## Proposed Solution

Fetch maximum window once.

Example:

```java
List<PDVHistoryEntity> allData =
        repository.fetchForLastNDays(180);
```

Build:

```java
Map<String, List<PDVHistoryEntity>>
```

once.

---

## Window Processing

Example:

```java
List<PDVHistoryEntity> data180 = tickerData;

List<PDVHistoryEntity> data60 =
        tickerData.subList(
                Math.max(0, tickerData.size() - 60),
                tickerData.size());

List<PDVHistoryEntity> data20 =
        tickerData.subList(
                Math.max(0, tickerData.size() - 20),
                tickerData.size());
```

Reuse already loaded data.

No additional database access.

---

## Expected Benefit

Current:

```text
7 DB queries
```

Expected:

```text
1 DB query
```

Savings:

```text
300-500 ms
```

---

# Optimization 3: UPSERT Only Changed Records

## Current Problem

Every execution updates all rows.

Current:

```text
13,000+ rows
```

are written every run.

Even when data is unchanged.

---

## Proposed Solution

Detect modified rows.

### Option A

Add:

```sql
last_calculated_date
```

column.

Skip rows already calculated.

---

### Option B

Add hash comparison.

Example:

```java
String hash =
        DigestUtils.md5Hex(
                score +
                trend +
                slope +
                intercept);
```

Store hash.

Only UPSERT when hash changes.

---

## Expected Benefit

Current:

```text
13,000 rows
```

Expected:

```text
1,800-2,000 rows
```

Savings:

```text
2.5 sec → 300-500 ms
```

---

# Optimization 4: Increase Multi-Row UPSERT Chunk Size

## Current State

Chunk size currently:

```java
500
```

or similar.

---

## Proposed Testing

Benchmark:

```java
500
1000
1500
2000
```

Example:

```java
private static final int CHUNK_SIZE = 1000;
```

---

## Notes

Monitor:

```text
Packet size
Memory usage
MySQL max_allowed_packet
```

---

## Expected Benefit

Additional:

```text
200-500 ms
```

improvement.

---

# Optimization 5: JDBC Driver Tuning

Verify datasource URL.

Recommended:

```properties
spring.datasource.url=
jdbc:mysql://localhost:3306/devilal
?rewriteBatchedStatements=true
&cachePrepStmts=true
&useServerPrepStmts=true
```

---

## Purpose

### rewriteBatchedStatements

Allows MySQL driver to optimize large inserts.

---

### cachePrepStmts

Caches prepared statements.

Reduces statement creation overhead.

---

### useServerPrepStmts

Uses server-side prepared statements.

Reduces parsing overhead.

---

## Expected Benefit

Minor but measurable.

Typically:

```text
5-15%
```

database improvement.

---

# Recommended Implementation Order

## Priority 1

Fetch Data Once

Expected Savings:

```text
300-500 ms
```

Low complexity.

---

## Priority 2

Parallel Window Processing

Expected Savings:

```text
700-1200 ms
```

High impact.

---

## Priority 3

Increase Chunk Size

Expected Savings:

```text
200-500 ms
```

Very easy implementation.

---

## Priority 4

JDBC Driver Tuning

Expected Savings:

```text
100-300 ms
```

Configuration only.

---

## Priority 5

UPSERT Only Changed Rows

Expected Savings:

```text
2 sec+
```

Highest gain.

Requires schema and logic changes.

---

# Expected Final Performance

After all optimizations:

| Operation             | Expected    |
| --------------------- | ----------- |
| Data Load             | 100-200 ms  |
| Mann-Kendall Analysis | 500-800 ms  |
| UPSERT                | 500-1500 ms |
| Total Runtime         | 2-3 sec     |

---

# Target Architecture

```text
Fetch 180-day data once
            │
            ▼
Build ticker cache
            │
            ▼
Process 10/20/30/40/50/60/180 windows in parallel
            │
            ▼
Generate changed records only
            │
            ▼
Chunked multi-row UPSERT (1000-2000 rows)
            │
            ▼
Complete in ~2-3 sec
```
