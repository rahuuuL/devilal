# Mann-Kendall History - Consolidated Functionalities

## Purpose
This document consolidates:
- Original Mann-Kendall History implementation requirements
- MK Generation Metadata Optimization requirements
- Additional clarifications and prompt updates provided during implementation

## Module Objective
Implement Mann-Kendall History generation in Java using the existing service:

```java
List<MannKendallAPIResponse> results =
    analyzeMannKendallForTicker.getMannKendallTrendAnalysis(fromDate, toDate);
```

## Database Design

### 1) mk_config
Current agreed model:
- days: INT PRIMARY KEY
- enabled: BOOLEAN
- description: VARCHAR

Notes:
- `days` is the configuration identity.
- No separate `id` column is used.

### 2) mk_result_history
Columns:
- ticker
- date
- days
- score
- trend
- h
- p
- z
- tau
- s
- var_s
- slope
- intercept

Primary key / uniqueness:
- Composite key: (ticker, date, days)

### 3) mk_generation_history
Columns:
- date: DATE
- days: INT
- status: VARCHAR (enum-driven)
- error_message: VARCHAR

Primary key / uniqueness:
- Composite key: (date, days)

Status enum (as requested):
- SUCESS
- FAILED

## Working-Day Utility
Use `LocalDate` and utility-driven calculation:

```text
weekendDays = floor(workingDays / 5) * 2
calendarDays = workingDays + weekendDays
fromDate = toDate.minusDays(calendarDays)
```

Also expose trading-day check utility (Mon-Fri true, Sat/Sun false).

## History Generation Flow (Optimized)
For a given processing date:
1. Read enabled configs from `mk_config` ordered by days.
2. Read `SUCESS` rows from `mk_generation_history` for that date.
3. Remove already successful windows from config processing list.
4. For each remaining window:
   - Compute fromDate using working-day utility.
   - Call existing Mann-Kendall service.
   - Save mapped results into `mk_result_history`.
   - Upsert generation status as `SUCESS` when done.
5. If one window fails:
   - Save `FAILED` with error message in `mk_generation_history`.
   - Continue with remaining windows.

Retry behavior:
- Future runs process only missing or FAILED windows (already SUCESS windows are skipped).

## Persistence Mapping
For each `MannKendallAPIResponse`:
- ticker -> ticker
- processing date -> date
- config.days -> days
- score -> score
- trend -> trend
- h -> h
- p -> p
- z -> z
- tau -> tau
- s -> s
- var_s -> var_s
- slope -> slope
- intercept -> intercept

## APIs
All APIs are under:
`/api/devilal/mann-kendall`

### A) Trend API
- GET `/log-close/trend?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD`

### B) History APIs
- POST `/history/generate`
  - Body:
    ```json
    {
      "date": "2026-07-26"
    }
    ```
- GET `/history?date=YYYY-MM-DD[&days=20][&tickers=RELIANCE,TCS]`

### C) Generation History Monitoring APIs
- GET `/generation-history?date=YYYY-MM-DD`
  - Read generation status by date.
- PUT `/generation-history/status?date=YYYY-MM-DD&days=120`
  - Update only generation status.
  - Body:
    ```json
    {
      "status": "SUCESS"
    }
    ```
    or
    ```json
    {
      "status": "FAILED"
    }
    ```

### D) MK Config APIs (days-based identity)
- POST `/config`
- PUT `/config/{days}`
- GET `/config`
- GET `/config/{days}`
- PATCH `/config/{days}/enabled`
- DELETE `/config/{days}`

## Kafka Integration

### Topic and message
- Topic for MK trigger: `mann-kendall-history`
- Message payload:

```json
{
  "date": "2026-07-26"
}
```

### Producer behavior from Data Sync
- Data Sync does not call MK service directly.
- After PDV persistence flow, publish MK trigger events.
- For each unique date present in PDV list, publish one event to `mann-kendall-history`.

### Consumer behavior
- `MannKendallHistoryKafkaConsumer` consumes `mann-kendall-history`.
- Extracts `date` and invokes history generation service for that date.

### Important guardrails
- `pdv-data` topic remains for full PDV payload consumption (ATR/RSI/VWAP).
- Do not publish date-only payloads to `pdv-data`.

## Error Handling Expectations
- Continue processing remaining MK windows if one fails.
- Store failure details in `mk_generation_history.error_message`.
- Kafka parsing/malformed payloads should be skipped safely where appropriate to avoid batch collapse.

## Performance/Processing Notes
- Config list is pre-filtered once by successful windows; no redundant per-iteration success-check branch.
- Stream-based unique date extraction is acceptable for readability; Kafka I/O dominates runtime.

## Implemented Migration Files
- `V1_013_mk_history.sql`
- `V1_014_mk_generation_history.sql`

## Summary of User Clarifications Included
1. `mk_result_history` uses composite primary key on ticker/date/days (no synthetic id).
2. Use metadata table `mk_generation_history` as source of truth for completed runs.
3. Add generation-history read and update APIs.
4. Keep generation status enum-limited to SUCESS and FAILED.
5. `mk_config` identity changed to `days` (no separate id).
6. Publish MK generation trigger per unique PDV date to `mann-kendall-history` topic.
