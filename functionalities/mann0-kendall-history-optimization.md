# Mann-Kendall History Generation Optimization (UPSERT Strategy)

## Current Findings

Performance analysis shows:

| Operation               | Time         |
| ----------------------- | ------------ |
| Data Fetch              | 30-150 ms    |
| MK Calculation          | 50-800 ms    |
| Object Mapping          | 1-3 ms       |
| DELETE Existing Records | 1100-1900 ms |
| saveAll()               | 1500-2000 ms |

The Mann-Kendall algorithm is performing well.

The primary bottleneck is database persistence.

---

# Current Problem

Current flow:

```java
deleteByDateAndDays(processingDate, config.getDays());

saveAll(recordsForWindow);
```

For every configured window:

```text
DELETE ~1900 rows
INSERT ~1900 rows
```

Repeated for:

```text
10 Days
20 Days
30 Days
40 Days
50 Days
60 Days
180 Days
```

Result:

```text
DELETE = 1-2 seconds
INSERT = 1.5-2 seconds
```

per window.

---

# New Strategy: UPSERT

## Principle

The table already guarantees uniqueness.

```sql
PRIMARY KEY (
    ticker,
    date,
    days
)
```

If a record already exists:

```text
ticker = RELIANCE
date = 2026-08-03
days = 60
```

we simply update it.

If it does not exist:

```text
insert it
```

No delete required.

---

# Expected Benefits

Remove:

```text
DELETE 1100ms - 1900ms
```

from every window.

Expected runtime reduction:

```text
30% - 50%
```

without changing the MK calculation logic.

---

# Database Index Optimization

Although delete is removed, query performance still benefits from a reporting index.

Add:

```sql
CREATE INDEX idx_date_days_ticker
ON mk_result_history(date, days, ticker);
```

---

## Liquibase

```xml
<changeSet id="mk-history-date-days-index" author="devilal">

    <createIndex
            tableName="mk_result_history"
            indexName="idx_date_days_ticker">

        <column name="date"/>
        <column name="days"/>
        <column name="ticker"/>

    </createIndex>

</changeSet>
```

Benefits:

```text
History API queries
Date-based reporting
Future maintenance operations
```

---

# Repository Persistence Strategy

## Remove

```java
mkResultHistoryRepository.deleteByDateAndDays(
        processingDate,
        config.getDays());
```

completely.

---

# Implement Native UPSERT

Spring JPA saveAll() does not generate MySQL ON DUPLICATE KEY UPDATE.

Create a custom repository method using a native query.

MySQL syntax:

```sql
INSERT INTO mk_result_history (
    ticker,
    date,
    days,
    score,
    trend,
    h,
    p,
    z,
    tau,
    s,
    var_s,
    slope,
    intercept
)
VALUES (...)
ON DUPLICATE KEY UPDATE
    score = VALUES(score),
    trend = VALUES(trend),
    h = VALUES(h),
    p = VALUES(p),
    z = VALUES(z),
    tau = VALUES(tau),
    s = VALUES(s),
    var_s = VALUES(var_s),
    slope = VALUES(slope),
    intercept = VALUES(intercept);
```

---

# Recommended Implementation

Create a custom repository implementation.

Example:

```java
public interface MkResultHistoryCustomRepository {

    void upsertBatch(
            List<MkResultHistoryEntity> entities);

}
```

Implementation:

```java
@Repository
@RequiredArgsConstructor
public class MkResultHistoryCustomRepositoryImpl
        implements MkResultHistoryCustomRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void upsertBatch(
            List<MkResultHistoryEntity> entities) {

        jdbcTemplate.batchUpdate(
                UPSERT_SQL,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement ps,
                            int i) throws SQLException {

                        MkResultHistoryEntity entity =
                                entities.get(i);

                        // set parameters

                    }

                    @Override
                    public int getBatchSize() {
                        return entities.size();
                    }
                });
    }
}
```

---

# Why JDBC Instead Of saveAll()

Current:

```java
saveAll(1900 entities)
```

causes:

```text
Entity management
Dirty checking
Persistence context growth
Hibernate overhead
```

Using JDBC batch:

```text
Direct SQL execution
No entity tracking
No dirty checking
No flush overhead
```

For bulk jobs this is significantly faster.

---

# Persistence Context Optimization

Current:

```java
@Transactional
public List<MkResultHistoryResponse> generateHistory(...)
```

During execution:

```text
~11,000 entities
```

remain managed.

Inject:

```java
@PersistenceContext
private EntityManager entityManager;
```

After each window:

```java
entityManager.flush();
entityManager.clear();
```

This prevents Hibernate from tracking thousands of objects.

---

# Save Strategy

Instead of:

```java
for (...) {

    upsertBatch(recordsForWindow);

}
```

accumulate all records:

```java
List<MkResultHistoryEntity> allRecords =
        new ArrayList<>();
```

then:

```java
allRecords.addAll(recordsForWindow);
```

for every window.

Finally:

```java
upsertBatch(allRecords);
```

once.

Expected:

```text
~13,000 records
single JDBC batch
```

instead of:

```text
7 separate persistence operations
```

---

# Performance Logging

Keep timing logs permanently.

```java
long start = System.currentTimeMillis();

upsertBatch(records);

log.info(
    "UPSERT rows={} time={}ms",
    records.size(),
    System.currentTimeMillis() - start
);
```

---

# Micrometer Metrics

Add metrics around persistence.

```java
@Timed(
    value = "mk.history.upsert",
    description = "MK history upsert"
)
```

and

```java
@Timed(
    value = "mk.history.generate",
    description = "MK history generation"
)
```

Metrics available through:

```text
/actuator/metrics
```

---

# Final Expected Architecture

```text
Load Data
    ↓
Calculate MK
    ↓
Create Entities
    ↓
Accumulate Results
    ↓
Single JDBC Batch UPSERT
    ↓
Commit
```

No deletes.

No re-inserts.

No unnecessary index churn.

No Hibernate bulk persistence overhead.

---

# Expected Outcome

Current:

```text
MK Calc         50-800 ms
DELETE          1100-1900 ms
INSERT          1500-2000 ms
```

Target:

```text
MK Calc         50-800 ms
UPSERT          300-1000 ms
```

Overall runtime reduction:

```text
40% - 70%
```

depending on database size and hardware.


# Multi-Row UPSERT Optimization for `mk_result_history`

## Current Implementation

Current implementation uses JDBC batching:

```java
jdbcTemplate.batchUpdate(
    "INSERT INTO mk_result_history (...) VALUES (?, ?, ?, ...) " +
    "ON DUPLICATE KEY UPDATE ...",
    batchPreparedStatementSetter
);
```

Although batching is enabled, MySQL still has to process every row individually during duplicate-key checks and updates.

Observed metrics:

```text
Rows Processed : ~13,175
UPSERT Time    : ~9.8 seconds
Throughput     : ~1,340 rows/sec
```

The database has only two indexes:

```sql
PRIMARY KEY (ticker, date, days)
KEY idx_date_days_ticker (date, days, ticker)
```

Therefore the remaining bottleneck is the UPSERT execution strategy itself rather than indexing.

---

# Recommended Optimization

Replace JDBC row-level batching with true multi-row UPSERT statements.

Instead of:

```sql
INSERT INTO mk_result_history (...)
VALUES (?, ?, ?, ...)
ON DUPLICATE KEY UPDATE ...
```

executed thousands of times,

generate a single statement containing hundreds of rows:

```sql
INSERT INTO mk_result_history
(
    ticker,
    date,
    days,
    score,
    trend,
    h,
    p,
    z,
    tau,
    s,
    var_s,
    slope,
    intercept
)
VALUES
    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?),
    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?),
    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?),
    ...
ON DUPLICATE KEY UPDATE
    score      = VALUES(score),
    trend      = VALUES(trend),
    h          = VALUES(h),
    p          = VALUES(p),
    z          = VALUES(z),
    tau        = VALUES(tau),
    s          = VALUES(s),
    var_s      = VALUES(var_s),
    slope      = VALUES(slope),
    intercept  = VALUES(intercept);
```

---

# Chunk Size

Do not build one giant SQL statement for all rows.

Process in chunks:

```text
500 rows per statement
or
1000 rows per statement
```

Example:

```text
Total Rows = 13,175

Chunk Size = 1000

Total SQL Statements ≈ 14
```

instead of potentially processing 13,175 UPSERT executions.

---

# Implementation Strategy

```java
List<MkResultHistoryEntity> rows = allRows;

for (int start = 0; start < rows.size(); start += 1000) {

    int end = Math.min(start + 1000, rows.size());

    List<MkResultHistoryEntity> chunk =
            rows.subList(start, end);

    executeMultiRowUpsert(chunk);
}
```

Where:

```java
executeMultiRowUpsert(List<MkResultHistoryEntity> chunk)
```

constructs:

```sql
INSERT INTO ...
VALUES (...), (...), (...), ...
ON DUPLICATE KEY UPDATE ...
```

and executes a single PreparedStatement.

---

# Expected Benefits

Current:

```text
13,175 rows
≈ 9.8 seconds
```

Expected after multi-row UPSERT:

```text
13,175 rows
≈ 2–5 seconds
```

depending on:

* MySQL hardware
* Disk performance
* Transaction settings
* Duplicate-key hit rate

---

# Additional Notes

This optimization should be implemented before exploring more advanced database tuning because:

1. Data fetch is already fast (30–200 ms).
2. Mann-Kendall calculations are already fast (< 1 second).
3. Object mapping is negligible (1–3 ms).
4. UPSERT operation consumes ~80–90% of total runtime.

Therefore the highest ROI optimization remaining is reducing the number of SQL executions by switching from row-level batching to true multi-row UPSERT statements.
