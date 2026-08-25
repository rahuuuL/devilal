# Price–Volume Pressure Profile (PVPP)

## Purpose

Build a daily price/volume framework that explains **what kind of pressure produced a price move**,
while remaining comparable across thousands of tickers.

The central idea:

> **Price = result, RVOL = participation/effort, CLV = where the session finished.**

The three dimensions are not immediately collapsed into one number — the raw vector is the primary
object, and a composite score is only introduced later once the raw features are validated.

---

## 0. Glossary

| Short form | Full form | Meaning |
|---|---|---|
| PVPP | Price–Volume Pressure Profile | This framework/module as a whole |
| RVOL | Relative Volume | `Volume[t] / SMA(Volume, days)` — today's volume vs its own recent average |
| CLV | Close Location Value | `(Close - Low) / (High - Low)` — where the close sits within the day's range |
| SMA | Simple Moving Average | Average of a value over the last N days |
| Z / Z-score | Standard score | `(value - mean) / stdDev`, used for cross-sectional normalization |
| PDV | Price/Delivery/Volume | The existing OHLCV-style market data source used as input |
| OHLCV | Open, High, Low, Close, Volume | Standard daily bar fields |
| JDBC | Java Database Connectivity | Low-level DB access API, used here for the custom batch UPSERT |
| JPA | Java Persistence API | ORM layer used for entities/repositories |
| DTO | Data Transfer Object | Request/response objects returned by the API, separate from entities |
| PK | Primary Key | Uniqueness constraint on a table |
| FK | Foreign Key | Reference to another table's key (used loosely here for `days` referencing `pvpp_config`) |
| UPSERT | Update or Insert | `INSERT ... ON DUPLICATE KEY UPDATE` — insert if new, update if existing |
| SUCESS | (as stored in `status`) | Generation status value indicating success (kept as-is for parity with the existing status column spelling) |

---

## 1. Core Daily Concepts

For every ticker, trading day, and configured lookback window (`days`):

```text
PressureRow = [ Return, RVOL, CLV ]
```

### Return

```text
Return = (Close[t] / Close[t-1]) - 1
```

The price result. Null when there is no previous close (first available day).

### RVOL (Relative Volume)

```text
RVOL = Volume[t] / SMA(Volume, days)
```

How unusual today's participation is relative to the ticker's own recent history. `days` is
configurable (typical horizons: 10, 20, 50, 100):

| Window | Purpose |
|---|---|
| 10 | Very recent participation |
| 20 | Primary daily pressure baseline |
| 50 | Intermediate participation regime |
| 100 | Longer-term participation regime |

Zero when fewer than `days` observations exist — never fabricate the baseline.

### CLV — Close Location Value

```text
CLV = (Close - Low) / (High - Low)
```

```text
0.00 → close at low
0.50 → close in middle
1.00 → close at high
```

`CLV = 0.5` when `High == Low` (zero-range day).

Optional directional form:

```text
CenteredCLV = 2 * CLV - 1
```

### Efficiency

```text
Efficiency = ABS(Return) / RVOL
```

How much price movement was achieved per unit of relative participation, for the row's `days`
window. Null when `RVOL` is null/zero.

### Why the Vector Matters — Effort vs Result

The same price move can tell different stories depending on participation:

| Price | RVOL | Possible story |
|---|---:|---|
| Down | Low | Quiet selling / weak demand |
| Up | High | Strong buying participation |
| Down | High | Strong supply / distribution |
| Up | Low | Price rising with limited participation |
| Small movement | High | High effort, small result; possible absorption |

Example — high effort, weak result:

```text
Return = -0.5%, RVOL = 3.0x, CLV = 0.10
→ high participation, small decline, close near low → possible absorption
```

Example — weak demand:

```text
Return = -3.0%, RVOL = 0.5x, CLV = 0.05
→ large decline despite low participation → lack of buyers, not aggressive selling
```

These are hypotheses to validate historically, not automatic conclusions.

### Participation Acceleration

```text
ParticipationAcceleration = RVOL10 / RVOL20
```

```text
> 1 → recent participation is stronger than the 20D baseline
< 1 → recent participation is weaker than the 20D baseline
```

This spans two different `days` windows, so it is not stored as a persisted column — it's computed
on demand by comparing the `days=10` and `days=20` rows for the same ticker/date (see §3.2).

---

## 2. Cross-Ticker Comparison

Raw values cannot be directly compared across tickers — they have different scales and
distributions. Normalize cross-sectionally, per trading date and per `days` window:

```text
ReturnZ     = cross-sectional Z-score of Return
LogRVOL     = log(RVOL)
LogRVOLZ    = cross-sectional Z-score of LogRVOL
CLVZ        = cross-sectional Z-score of CLV
```

`log(RVOL)` is used because RVOL is heavily right-skewed (0.5, 1, 2, 5, 10, 20, 100...); taking the
log before normalizing reduces the influence of extreme volume outliers.

A percentile representation is also useful for ranking (e.g. "RVOL percentile = 98" means higher
than 98% of the universe). Both percentile and Z-score representations can be stored.

### Vector Similarity

Once normalized, ticker vectors `[ReturnZ, LogRVOLZ, CLVZ]` can be compared with Euclidean
distance to find tickers behaving similarly on a given date, supporting clustering, peer
comparison, and unusual-behavior detection. Only compare distances after normalization.

---

## 3. Pressure Type Classification

The normalized vector is converted into a descriptive pressure type — but this label is a
**derived read-time view, not stored ground truth**. Rule thresholds change as they get validated,
and a label computed under an old rule set must not linger in the table looking authoritative
after the rules evolve. So classification is computed on the fly by combining the persisted raw
features with whichever rule configuration is currently active:

```text
pressure_data
       +
active rule configuration
       ↓
classification
```

`pressure_data` = the stored `return_z`/`log_rvol_z`/`clv_z` (and related raw fields) for a
`(ticker, date, days)` row. The active rule configuration is a swappable set of thresholds
(initially hardcoded constants, later a small config the classifier reads from) — nothing about
the classification itself is persisted in `pvpp_result_history`.

| Type | Pattern | Interpretation |
|---|---|---|
| Aggressive Selling | ReturnZ ↓↓↓, RVOLZ ↑↑↑, CLVZ ↓↓↓ | Strong evidence of supply/distribution |
| Weak Demand | ReturnZ ↓↓↓, RVOLZ ↓, CLVZ ↓ | Large decline without real participation — lack of buyers |
| High-Effort / Weak-Result | RVOLZ ↑↑↑, ABS(ReturnZ) small | Large participation, little price progress — possible absorption |
| Strong Buying | ReturnZ ↑↑, RVOLZ ↑↑, CLVZ ↑↑ | Strong upside with strong participation, close near high |
| Quiet Markup | ReturnZ ↑↑, RVOLZ ↓, CLVZ ↑ | Price rising despite low participation |
| Quiet Weakness | ReturnZ slightly ↓, RVOLZ ↓ | Low-participation weakness |

```java
String classify(double returnZ, double rvolZ, double clvZ) {
    if (returnZ <= -1 && rvolZ >= 1 && clvZ <= -1) return "AGGRESSIVE_SELLING";
    if (returnZ <= -1 && rvolZ <= -0.5 && clvZ <= -0.5) return "WEAK_DEMAND";
    if (Math.abs(returnZ) < 0.5 && rvolZ >= 1) return "HIGH_EFFORT_WEAK_RESULT";
    if (returnZ >= 1 && rvolZ >= 1 && clvZ >= 1) return "STRONG_BUYING";
    if (returnZ >= 1 && rvolZ <= -0.5 && clvZ >= 0.5) return "QUIET_MARKUP";
    if (returnZ < 0 && rvolZ <= -0.5) return "QUIET_WEAKNESS";
    return "NEUTRAL";
}
```

This `classify(...)` function is called at read time (API response mapping / `/rank` / `/similar`),
never during generation — regenerating history never needs to touch a `pressure_type` column, and
changing the rule thresholds instantly changes classification for all historical rows without a
backfill. A scalar `pressure_score` can be added later once the raw fields (Return/RVOL/CLV and
their Z-scores) have been validated — it must not replace them, and like `pressure_type` it should
remain a derived/read-time value rather than persisted truth unless a strong reason emerges to cache it.

---

## 4. Historical Pressure Regime

Beyond the daily row, aggregate features over multiple horizons (10D/20D/40D/60D) help determine
whether pressure is building, weakening, persistent, or reversing:

```text
Average Return, Median RVOL, Average CLV, Average Efficiency
% high-RVOL down days, % high-RVOL up days
% down days closing near low, % up days closing near high
Average ReturnZ, Average LogRVOLZ, Average CLVZ
```

Example:

```text
10D pressure = -0.72
20D pressure = -0.30
40D pressure = +0.18
→ recent selling pressure exists, but the longer-term regime was previously positive
```

These aggregates are computed at query time from `pvpp_result_history` rather than persisted, since
they are derived views over the stored daily rows.

---

## 5. Package Structure

```
src/main/java/com/terminal_devilal/business_tools/pvpp/
├── calculator/
│   ├── PvppCalculator.java                 (interface)
│   ├── PvppCalculatorImpl.java
│   └── PvppCalcResult.java
├── controller/
│   └── PvppController.java
├── dto/
│   ├── PvppResponse.java
│   ├── PvppHistoryGenerateRequest.java
│   ├── PvppConfigRequest.java
│   ├── PvppConfigResponse.java
│   ├── PvppGenerationHistoryStatusUpdateRequest.java
│   └── PvppResultHistoryResponse.java
├── entity/
│   ├── PvppConfigEntity.java
│   ├── PvppGenerationHistoryEntity.java
│   ├── PvppGenerationHistoryId.java
│   ├── PvppGenerationStatus.java            (enum: SUCESS, FAILED)
│   ├── PvppResultHistoryEntity.java
│   └── PvppResultHistoryId.java
├── repository/
│   ├── PvppConfigRepository.java
│   ├── PvppGenerationHistoryRepository.java
│   ├── PvppResultHistoryCustomRepository.java   (interface)
│   ├── PvppResultHistoryCustomRepositoryImpl.java (JDBC UPSERT)
│   └── PvppResultHistoryRepository.java
└── service/
    ├── AnalyzePvppForTicker.java
    ├── PvppHistoryService.java
    └── PvppConfigService.java
```

Conventions:
- No Lombok — manual getters/setters like the rest of `business_tools`.
- `java.time.LocalDate` everywhere, `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` on controller params.
- `@Timed(value = "...", description = "...")` (Micrometer) on generation + upsert methods.
- Composite keys via `@IdClass`, not surrogate `id` columns.

---

## 6. Database Design

### 6.1 `pvpp_config`
The config identity is the RVOL/lookback window in days, not a synthetic id.

| Column        | Type          | Notes                                  |
|---------------|---------------|-----------------------------------------|
| `days`        | INT PK        | Historical window used for RVOL/percentile universe (e.g. 10/20/50/100) |
| `enabled`     | BOOLEAN       | default TRUE                            |
| `description` | VARCHAR(255)  | optional                                |

### 6.2 `pvpp_result_history`
One row per `(ticker, date, days)`. `days` identifies which lookback window the row was generated
for — a single generic `rvol` column is used, rather than separate `rvol_10`/`rvol_20`/etc columns.

| Column          | Type          |
|-----------------|---------------|
| `ticker`        | VARCHAR(50) NOT NULL |
| `date`          | DATE NOT NULL |
| `days`          | INT NOT NULL — FK/config identity, e.g. 10/20/50/100 |
| `return_pct`    | DOUBLE |
| `volume`        | BIGINT |
| `rvol`          | DOUBLE — `Volume[t] / SMA(Volume, days)` for this row's `days` |
| `clv`           | DOUBLE |
| `centered_clv`  | DOUBLE |
| `efficiency`    | DOUBLE — `ABS(return_pct) / rvol` for this row's `days` |
| `return_z`      | DOUBLE |
| `log_rvol_z`    | DOUBLE |
| `clv_z`         | DOUBLE |
| `pressure_score`| DOUBLE (nullable — added later per spec section 21) |

Primary key: `(ticker, date, days)`.

No `pressure_type` column: that label is derived at read time from `return_z`/`log_rvol_z`/`clv_z`
plus the active rule configuration (see §3), so it is never persisted or backfilled.

`return_pct`, `volume`, `clv`, `centered_clv` are not window-dependent but are still stored per row
(repeated across each `days` row for the same ticker/date) for simplicity of querying a single
window's row. `ParticipationAcceleration` (`RVOL10 / RVOL20`) is not stored — it's computed by
joining the `days=10` and `days=20` rows for the same `(ticker, date)`.

Index:

```sql
CREATE INDEX idx_pvpp_date_days_ticker ON pvpp_result_history(date, days, ticker);
```

### 6.3 `pvpp_generation_history`
Tracks which `(date, days)` windows have already been generated, so re-runs only process missing or
failed windows.

| Column          | Type |
|-----------------|------|
| `date`          | DATE NOT NULL |
| `days`          | INT NOT NULL — which `pvpp_config` window this generation run covers |
| `status`        | VARCHAR(20) NOT NULL — enum `SUCESS` / `FAILED` |
| `error_message` | VARCHAR(1024) |

Primary key: `(date, days)`.

### 6.4 Migration Files (next available V1 numbers after `V1_014`)

`V1_015_pvpp_history.sql`:
```sql
CREATE TABLE IF NOT EXISTS pvpp_config (
    days INT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS pvpp_result_history (
    ticker VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    days INT NOT NULL,
    return_pct DOUBLE,
    volume BIGINT,
    rvol DOUBLE,
    clv DOUBLE,
    centered_clv DOUBLE,
    efficiency DOUBLE,
    return_z DOUBLE,
    log_rvol_z DOUBLE,
    clv_z DOUBLE,
    pressure_score DOUBLE,
    PRIMARY KEY (ticker, date, days)
);

CREATE INDEX idx_pvpp_date_days_ticker ON pvpp_result_history(date, days, ticker);
```

`V1_016_pvpp_generation_history.sql`:
```sql
CREATE TABLE IF NOT EXISTS pvpp_generation_history (
    date DATE NOT NULL,
    days INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1024),
    PRIMARY KEY (date, days)
);
```

Register in `db.changelog-v1.xml`:

```xml
<changeSet id="V1-015" author="devilal">
    <preConditions onFail="MARK_RAN">
        <not>
            <indexExists tableName="pvpp_result_history" indexName="idx_pvpp_date_days_ticker"/>
        </not>
    </preConditions>
    <sqlFile path="db/migration/V1_015_pvpp_history.sql" relativeToChangelogFile="false"/>
</changeSet>

<changeSet id="V1-016" author="devilal">
    <sqlFile path="db/migration/V1_016_pvpp_generation_history.sql" relativeToChangelogFile="false"/>
</changeSet>
```

---

## 7. Calculation Logic

### 7.1 `PvppCalculator` — per-ticker, per-`days`-window daily series → one row per window

Input: sorted `List<PriceDeliveryVolumeEntity>` for one ticker, covering at least `days` trading
days before the target date, plus the enabled `pvpp_config` window list (e.g. 10/20/50/100).

For each enabled config `days`, steps per ticker/date `t`:
1. `Return = Close[t]/Close[t-1] - 1` (zero if no previous close) — same for every `days` row.
2. `RVOL = Volume[t] / SMA(Volume, days)`; do not calc if fewer than `days` observations.
3. `CLV = (Close - Low) / (High - Low)`; `CLV = 0.5` if `High == Low` — same for every `days` row.
4. `CenteredCLV = 2*CLV - 1` — same for every `days` row.
5. `Efficiency = ABS(Return) / RVOL` (zero if `RVOL` zero) — window-specific per row.
6. `LogRVOL = log(RVOL)`

Guard nulls and insufficient history explicitly; never fabricate a baseline. One calculation pass
per enabled `days` config produces one persisted row per window.

`ParticipationAcceleration` is not part of the per-window calculator output — it's a cross-window
ratio, computed on demand from two `pvpp_result_history` rows (see §6.2).

### 7.2 Cross-Sectional Normalization

Per trading date and per `days` window, across all tickers:

- `ReturnZ`, `LogRVOLZ`, `CLVZ` = z-score of each metric across all tickers, computed separately
  for each `days` window (RVOL/LogRVOL differ per window; Return/CLV z-scores end up identical
  across windows for the same date, which is expected).
- Add `mean`, `stdDev`, `zScore` helpers to
  `com.terminal_devilal.utils.common_calcs.StatisticsUtils` if not already present.

### 7.3 Edge Cases

- **Zero range** (`High == Low`): `CLV = 0.5`.
- **First available day**: `Return = null` (no previous close).
- **Insufficient RVOL history**: if fewer than `days` observations exist, `RVOL = null` — do not
  fabricate the baseline.
- **Extreme volume**: use `log(RVOL)` before cross-sectional Z-score normalization.

---

## 8. Service Layer Flow

`PvppHistoryService.generateHistory(LocalDate processingDate)`:

1. Load enabled `pvpp_config` rows ordered by `days` (e.g. 10/20/50/100).
2. Read `pvpp_generation_history` for `processingDate`; skip any `days` already marked `SUCESS`.
3. Fetch PDV data once for all tickers, using the max configured `days` back from `processingDate`;
   slice per-window data from the same in-memory dataset instead of querying per window.
4. For each remaining (not-yet-`SUCESS`) `days` window, and for each ticker, compute that window's
   row via `PvppCalculator` (process windows and/or tickers in parallel).
5. Second pass per `days` window: compute cross-sectional `ReturnZ/LogRVOLZ/CLVZ` across all
   ticker rows for `(processingDate, days)`. `pressure_type` is not computed or stored here — it's
   derived at read time from these Z-scores (see §3).
6. Batch UPSERT all rows (across all windows) via `PvppResultHistoryCustomRepositoryImpl`, chunked.
7. Per window: upsert `pvpp_generation_history` = `SUCESS`, or `FAILED` + `error_message` on
   exception — one window's failure does not block the others.

### 8.1 Custom JDBC UPSERT (`PvppResultHistoryCustomRepositoryImpl`)

Chunked multi-row `INSERT ... ON DUPLICATE KEY UPDATE`, chunk size configurable via
`pvpp.history.upsert.chunk-size` (default 1000):

```sql
INSERT INTO pvpp_result_history (
    ticker, date, days, return_pct, volume, rvol,
    clv, centered_clv, efficiency,
    return_z, log_rvol_z, clv_z, pressure_score
) VALUES
    (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?),
    ...
ON DUPLICATE KEY UPDATE
    return_pct = VALUES(return_pct),
    volume = VALUES(volume),
    rvol = VALUES(rvol),
    clv = VALUES(clv),
    centered_clv = VALUES(centered_clv),
    efficiency = VALUES(efficiency),
    return_z = VALUES(return_z),
    log_rvol_z = VALUES(log_rvol_z),
    clv_z = VALUES(clv_z),
    pressure_score = VALUES(pressure_score);
```

Annotate with `@Timed(value = "pvpp.history.upsert", description = "PVPP history upsert")`.

No delete+insert — use `ON DUPLICATE KEY UPDATE` directly, and avoid JPA `saveAll()` for bulk rows
(no dirty checking / persistence-context overhead for a job writing thousands of rows).

---

## 9. APIs

All under: `/api/devilal/pvpp`

### A) On-demand vector API
- `GET /vector?date=YYYY-MM-DD&days=20&tickers=RELIANCE,TCS` — compute (not persisted) PVPP vector
  for the given tickers/date/window.

### B) History generation
- `POST /history/generate`
  ```json
  { "date": "2026-08-25" }
  ```
  → `List<PvppResultHistoryResponse>` (201)

### C) History query
- `GET /history?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD[&days=20][&tickers=RELIANCE,TCS]`
  - Inclusive range, `toDate` must not be before `fromDate`.
  - `days` optional (omit to get all enabled windows for each date).
  - Sorted by `date` descending at the DB level (`findByDateBetween...OrderByDateDesc`).

### D) Generation history monitoring
- `GET /generation-history?date=YYYY-MM-DD`
- `PUT /generation-history/status?date=YYYY-MM-DD&days=100`
  ```json
  { "status": "SUCESS" }
  ```

### E) Config APIs (days-based identity)
- `POST /config`
- `PUT /config/{days}`
- `GET /config`
- `GET /config/{days}`
- `PATCH /config/{days}/enabled`
- `DELETE /config/{days}`

### F) Similarity / ranking

- `GET /similar?ticker=RELIANCE&date=YYYY-MM-DD&days=20&limit=10` — nearest tickers by Euclidean
  distance over `[return_z, log_rvol_z, clv_z]` for that `(date, days)`.
- `GET /rank?date=YYYY-MM-DD&days=20&limit=50` — scalar-ranked list plus a pressure-profile
  explanation per ticker (ranking + explanation instead of a black-box score), once
  `pressure_score` is introduced.

---

## 10. Kafka Integration

- Topic: `pvpp-history`, 8 partitions, replica 1.
- Producer: after PDV persistence, publish one event per unique date to `pvpp-history`, gated by a
  flag `pvpp-sync.enabled`.
  - Payload: `{"date":"YYYY-MM-DD"}`.
- Consumer: `PvppHistoryKafkaConsumer` — batch listener with a pending-dates queue and a scheduled
  flush (`BATCH_SIZE=100`, `FLUSH_DELAY_MS=2000`), deduplicating dates and processing each unique
  date asynchronously (`CompletableFuture.runAsync`, joined before completing the batch).
  - `@KafkaListener(topics = "pvpp-history", groupId = "devilal-group", containerFactory = "batchFactory", concurrency = "8")`

Guardrail: keep `pdv-data` topic reserved for full PDV payload consumption (ATR/RSI/VWAP/MK); do
not overload it with PVPP triggers — use the dedicated `pvpp-history` topic instead.

---

## 11. Performance Notes

The generation job processes multiple `days` windows per run, so:

1. **Fetch data once**: one query for the max configured `days` lookback across all tickers, slice
   per ticker/window in memory instead of querying per window.
2. **Parallel window processing**: process each `days` window concurrently
   (`CompletableFuture`/`ExecutorService`).
3. **Chunked multi-row UPSERT**: 1000-row chunks via `JdbcTemplate`, not `saveAll()`.
4. **No delete+insert**: use `ON DUPLICATE KEY UPDATE` directly.
5. Add `rewriteBatchedStatements=true&cachePrepStmts=true&useServerPrepStmts=true` to the JDBC URL
   if not already present.

Expected profile: PDV fetch ~100-200ms; calculation per window is lightweight (simple arithmetic,
no iterative statistics); UPSERT dominates runtime and scales with `tickers × windows` rows per run.

---

## 12. Implementation Order

1. Migrations: `pvpp_config`, `pvpp_result_history`, `pvpp_generation_history` + index.
2. Entities + repositories (JPA + custom JDBC upsert repo).
3. `PvppCalculator` + `PvppCalculatorImpl` (Return/RVOL/CLV/Efficiency per `days` window).
4. Cross-sectional normalization helper (§7.2) + read-time `pressure_type` classifier (§3) — the
   classifier is not wired into the persistence path.
5. `PvppHistoryService` (generate/query/generation-history) + `PvppConfigService`.
6. `PvppController` (all endpoints in §9, except `/similar` and `/rank` initially).
7. Kafka topic/producer/consumer wiring, gated by `pvpp-sync.enabled=false` default.
8. Add `/similar` and `/rank` endpoints once raw features are validated, followed by the optional
   composite `pressure_score`.
