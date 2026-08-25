# PVPP — Removal Instructions: Vector API + Pressure Type Classification/Scoring

## Scope

This removes two already-implemented pieces from the PVPP module:

1. The on-demand **`/vector`** endpoint (compute-only, no persistence).
2. The **Pressure Type** classification/scoring layer: `classify(...)`, `pressure_type`,
   `pressure_score`, and the `/rank` and `/similar` endpoints (and the Vector Similarity /
   Euclidean-distance logic backing `/similar`).

Everything else (config, daily/window generation, `pvpp_daily`, `pvpp_result_history`,
generation-history tracking, history query, Kafka trigger) stays as-is. Do not touch those paths
except where explicitly listed below (e.g. dropping the `pressure_score` column).

---

## 1. Database

### 1.1 Drop `pressure_score` column

`pvpp_result_history.pressure_score` is no longer written or read anywhere. Add a new migration
(next available `V1_0XX` number) rather than editing the original migration file:

```sql
ALTER TABLE pvpp_result_history DROP COLUMN pressure_score;
```

Register it as a new changeSet in `db.changelog-v1.xml`, same pattern as the existing PVPP
changeSets.

### 1.2 No other schema changes

`pressure_type` was never a persisted column (§3 of the spec explicitly kept it read-time-only), so
there's nothing to drop for that. `pvpp_daily`, `pvpp_config`, `pvpp_generation_history` are
unaffected.

---

## 2. Code to delete entirely

Remove these files/classes if they exist:

- Any `PvppVectorRequest`/`PvppVectorResponse` (or similarly named) DTO backing `/vector`.
- Any `PressureType` enum or `PvppPressureClassifier` / `classify(...)` implementation
  (wherever the rule-threshold function from spec §3 was placed — likely in `calculator/` or a
  dedicated `classification/` sub-package).
- Any `PvppSimilarityService` / Euclidean-distance helper used only by `/similar`.
- Any `PvppRankResponse` / rank-explanation DTO used only by `/rank`.
- Any rule-configuration entity/table/service that existed solely to feed the classifier (e.g. a
  `pvpp_classification_rules` table or config class) — confirm it isn't reused elsewhere before
  deleting.

If any of the above logic is embedded inline inside a larger class (e.g. `classify(...)` as a
private method on `PvppHistoryService` rather than its own class), delete just that method/block
instead of the whole class.

---

## 3. Controller — remove endpoints

In `PvppController`, remove the handler methods for:

- `GET /vector`
- `GET /similar`
- `GET /rank`

Leave everything else (`POST /history/generate`, `GET /history`, `GET /generation-history`,
`PUT /generation-history/status`, `/config` CRUD) untouched.

---

## 4. Service layer — remove dead calls

- In `PvppHistoryService` (or wherever `/rank`/`/similar` read their data from), remove any method
  that assembles a classification/ranking/similarity response. Confirm none of this logic is also
  reused by the generation flow (`generateHistory(...)`) before deleting — it shouldn't be, since
  classification was always read-time-only per the original spec, but verify.
- Remove any reference to `pressure_score` in the batch UPSERT SQL/`JdbcTemplate` calls inside
  `PvppResultHistoryCustomRepositoryImpl` (both the `INSERT` column list and the
  `ON DUPLICATE KEY UPDATE` clause).
- Remove `pressure_score` from `PvppCalcResult` (or equivalent calculator output object) if it was
  ever populated there — it shouldn't have been (spec kept it derived), but check.
- Remove `pressure_score` from `PvppResultHistoryResponse` (or equivalent) DTO and any mapping code
  that sets it.

---

## 5. Entity/repository cleanup

- Remove `pressureScore` field + getter/setter from `PvppResultHistoryEntity`.
- If a custom repository method exists solely to support `/similar` or `/rank` (e.g. a bulk
  Z-score fetch across the full ticker universe for a date), remove it — confirm it isn't also used
  by `/history` before deleting.

---

## 6. Tests

Remove or update:

- Any unit test for `classify(...)` / the pressure-rule thresholds.
- Any controller/integration test hitting `/vector`, `/similar`, or `/rank`.
- Any test asserting `pressure_score` is present in a UPSERT or API response — update fixtures/
  assertions that include `pressure_score` in expected JSON or SQL.

---

## 7. Config / properties

Remove any application properties tied only to the removed features, e.g.:

- Classification rule-threshold config keys (if externalized, e.g.
  `pvpp.classification.rules.*`).
- Any `pvpp.rank.*` / `pvpp.similar.*` limit/default properties.

Leave `pvpp.history.upsert.chunk-size` and `pvpp-sync.enabled` as-is.

---

## 8. Verification checklist

After removal, confirm:

- [ ] `pvpp_result_history` no longer has a `pressure_score` column in the DB.
- [ ] No remaining reference to `pressure_score`, `pressure_type`, `classify`, `PvppSimilarity`,
      `PvppRank`, or `PvppVector` anywhere in `grep -ri` across the `pvpp` package.
- [ ] `/vector`, `/similar`, `/rank` return 404 (route removed), not just an empty/error body.
- [ ] `POST /history/generate` and `GET /history` still work unchanged — run existing integration
      tests for these to confirm no accidental coupling was broken.
- [ ] No orphaned DTOs, enums, or config classes left behind with zero references (a final
      `grep`/IDE "find usages" pass on anything you weren't 100% sure about).