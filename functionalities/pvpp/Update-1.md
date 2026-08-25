# PVPP — Removal Instructions: Z-Scores + Table Consolidation

## Scope

With `/vector`, `/similar`, `/rank`, and the Pressure classification layer already removed (see
prior removal doc), the only consumer of `return_z`, `clv_z`, and `log_rvol_z` is gone. Cross-
sectional normalization has no remaining purpose, so this removes it entirely — and since the
`pvpp_daily` / `pvpp_result_history` split existed mainly to avoid recomputing those Z-scores
redundantly per window, that reason is now gone too. **Consolidate back into a single history
table.**

End state: one table, `pvpp_result_history`, holding raw features only — no Z-scores, no
normalization, no second table.

---

## 1. Database

### 1.1 New consolidated `pvpp_result_history`

Add a new migration (next available `V1_0XX`):

```sql
-- Drop the now-unnecessary split table
DROP TABLE IF EXISTS pvpp_daily;

-- Rebuild pvpp_result_history without Z-score columns, folding pvpp_daily's fields back in
ALTER TABLE pvpp_result_history
    DROP COLUMN log_rvol_z,
    ADD COLUMN return_pct DOUBLE,
    ADD COLUMN volume BIGINT,
    ADD COLUMN clv DOUBLE,
    ADD COLUMN centered_clv DOUBLE;
```

If you'd rather migrate cleanly instead of altering in place (recommended if there's any live data
worth preserving, since the join key differs — `pvpp_daily` is keyed by `(ticker, date)` and needs
to be fanned out to every matching `(ticker, date, days)` row):

```sql
CREATE TABLE pvpp_result_history_new (
    ticker VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    days INT NOT NULL,
    return_pct DOUBLE,
    volume BIGINT,
    clv DOUBLE,
    centered_clv DOUBLE,
    rvol DOUBLE,
    efficiency DOUBLE,
    PRIMARY KEY (ticker, date, days)
);

INSERT INTO pvpp_result_history_new
SELECT h.ticker, h.date, h.days, d.return_pct, d.volume, d.clv, d.centered_clv, h.rvol, h.efficiency
FROM pvpp_result_history h
JOIN pvpp_daily d ON d.ticker = h.ticker AND d.date = h.date;

DROP TABLE pvpp_result_history;
DROP TABLE pvpp_daily;
RENAME TABLE pvpp_result_history_new TO pvpp_result_history;

CREATE INDEX idx_pvpp_date_days_ticker ON pvpp_result_history(date, days, ticker);
```

### 1.2 Final `pvpp_result_history` shape

| Column         | Type |
|----------------|------|
| `ticker`       | VARCHAR(50) NOT NULL |
| `date`         | DATE NOT NULL |
| `days`         | INT NOT NULL |
| `return_pct`   | DOUBLE |
| `volume`       | BIGINT |
| `clv`          | DOUBLE |
| `centered_clv` | DOUBLE |
| `rvol`         | DOUBLE |
| `efficiency`   | DOUBLE |

Primary key: `(ticker, date, days)`. Same index as before:
`CREATE INDEX idx_pvpp_date_days_ticker ON pvpp_result_history(date, days, ticker);`

`return_pct`/`volume`/`clv`/`centered_clv` are duplicated across each ticker's window rows again
(back to the original pre-split shape) — this is now an accepted, deliberate trade-off since
there's no longer a Z-score computation cost tied to the split, only a small amount of disk
duplication (~70MB/year at 3000 tickers × 4 windows, per earlier estimate). Not worth the extra
table/join for that alone.

`pvpp_config` and `pvpp_generation_history` are unaffected by this change.

---

## 2. Code to delete entirely

- `PvppDailyEntity`, `PvppDailyRepository`, and any `PvppDailyResponse`/DTO tied only to
  `pvpp_daily`.
- Any cross-sectional normalization class/method (wherever `ReturnZ`/`CLVZ`/`LogRVOLZ` were
  computed — likely a `PvppNormalizationService` or a method on `PvppHistoryService`).
- `zScoreMap` / `percentileMap` / `zScore` helpers on `StatisticsUtils` **if they have no other
  caller outside PVPP** — check for other usages in `common_calcs` before deleting; if shared,
  leave the utility method and just remove the PVPP call site.

---

## 3. Calculator (`PvppCalculator` / `PvppCalculatorImpl`)

Remove the window-independent vs. window-dependent split that existed to route values to two
tables. Collapse back to one pass per ticker producing one row per enabled `days` window directly:

```text
For each ticker, for each enabled days window:
1. Return = Close[t]/Close[t-1] - 1        (null if no previous close)
2. CLV = (Close - Low) / (High - Low)      (0.5 if High == Low)
3. CenteredCLV = 2*CLV - 1
4. RVOL = Volume[t] / SMA(Volume, days)    (null if fewer than `days` observations)
5. Efficiency = ABS(Return) / RVOL         (null if RVOL null/zero)
```

Note `Return`/`CLV`/`CenteredCLV` are still identical across a ticker's window rows for the same
date — that's fine and no longer needs special handling; just compute them once per ticker and
copy into each window's row object before returning, same as before, just without a second
Z-score pass or a second table target.

Remove any `LogRVOL` computation — it was only an intermediate for `LogRVOLZ`, which no longer
exists.

---

## 4. Service layer (`PvppHistoryService`)

- Remove the cross-sectional normalization pass (the former step 5 in `generateHistory(...)`:
  "compute ReturnZ/CLVZ once per date, LogRVOLZ once per window"). Delete it outright.
- Collapse the two batch UPSERTs (`pvpp_daily` + `pvpp_result_history`) back into a single UPSERT
  into `pvpp_result_history`.
- Per-ticker parallelism, per-ticker error handling, and the `IN_PROGRESS` claim guard for
  concurrent generation triggers are unaffected by this change — keep those as-is.

---

## 5. Repository (`PvppResultHistoryCustomRepositoryImpl`)

Single UPSERT statement, no Z-score columns:

```sql
INSERT INTO pvpp_result_history (
    ticker, date, days, return_pct, volume, clv, centered_clv, rvol, efficiency
) VALUES
    (?, ?, ?, ?, ?, ?, ?, ?, ?),
    ...
ON DUPLICATE KEY UPDATE
    return_pct = VALUES(return_pct),
    volume = VALUES(volume),
    clv = VALUES(clv),
    centered_clv = VALUES(centered_clv),
    rvol = VALUES(rvol),
    efficiency = VALUES(efficiency);
```

Delete the now-unused `pvpp_daily` UPSERT statement and its repository method entirely.

---

## 6. DTOs / API responses

- Remove `return_z`, `clv_z`, `log_rvol_z` fields from `PvppResultHistoryResponse` (or equivalent)
  and any mapping code that sets them.
- `GET /history` response shape simplifies to a flat read of the single table — no join needed
  anymore. Update the query/repository method used by `/history` to read directly from
  `pvpp_result_history` instead of joining `pvpp_daily`.

---

## 7. Entity cleanup

- Remove `returnZ`, `clvZ` fields from whatever entity backed `pvpp_daily` (being deleted, §2).
- Remove `logRvolZ` field + getter/setter from `PvppResultHistoryEntity`.
- Add `returnPct`, `volume`, `clv`, `centeredClv` fields + getters/setters to
  `PvppResultHistoryEntity` (moving back from the deleted `PvppDailyEntity`).

---

## 8. Tests

- Delete/update any test asserting `return_z`/`clv_z`/`log_rvol_z` values in calculator output,
  UPSERT SQL, or `/history` response JSON.
- Delete any test targeting `PvppDailyRepository`/`PvppDailyEntity` or the two-table join in
  `/history`.
- Update `/history` integration tests to expect the flattened single-table response shape.
- Update generation-flow tests (`generateHistory`) to expect one UPSERT call instead of two.

---

## 9. Config / properties

No PVPP-specific properties were tied only to normalization, so nothing to remove here beyond what
was already covered in the prior removal doc (classification/rank/similar properties).

---

## 10. Verification checklist

- [ ] `pvpp_daily` table dropped; only `pvpp_config`, `pvpp_result_history`,
      `pvpp_generation_history` remain.
- [ ] `pvpp_result_history` has no `return_z`, `clv_z`, or `log_rvol_z` column.
- [ ] No remaining reference to `ReturnZ`, `CLVZ`, `LogRVOLZ`, `LogRVOL`, `zScore`, `PvppDaily`,
      or normalization anywhere in `grep -ri` across the `pvpp` package.
- [ ] `PvppHistoryService.generateHistory(...)` issues exactly one UPSERT statement per chunk, not
      two.
- [ ] `GET /history` still returns `return_pct`/`volume`/`clv`/`centered_clv`/`rvol`/`efficiency`
      correctly, now from a single-table read with no join.
- [ ] `POST /history/generate`, `/config` CRUD, and `/generation-history` endpoints still pass
      existing integration tests unchanged.