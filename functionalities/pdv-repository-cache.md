# PDV Repository Cache Implementation Design

## Overview

Implement an abstraction layer over `PDVRepository` that provides in-memory caching and future pluggability for alternative cache providers (Chronicle Map, Hazelcast, Redis, etc.).

The objective is:

* Reduce repeated database reads for historical PDV data.
* Support efficient retrieval by ticker and date range.
* Maintain compatibility with existing repository method signatures.
* Allow future cache provider replacement with minimal changes to business services.
* Persist cache snapshots using Chronicle Map to reduce application startup time.
* Support cache refresh and cache rebuild through REST endpoints.
* Make cache preload duration configurable through application properties.

---

# Architecture

```text
Business Service
       |
       V
PDVCacheService (Interface)
       |
       +-------------------+
       |                   |
       V                   V
InMemoryCacheService   Future Cache Provider
(Current)              (Redis/Hazelcast/etc.)
       |
       V
PDVRepository
```

Business services must never directly call `PDVRepository`.

All data access must go through:

```java
PDVCacheService
```

This ensures future cache provider replacement only requires implementation changes inside the cache module.

---

# Cache Data Structure

## Primary Cache

```java
Map<String, ArrayList<PDVHistoryEntity>> cache;
```

Key:

```text
Ticker
```

Value:

```text
Sorted list of PDVHistoryEntity
```

Example:

```text
RELIANCE
[
  2025-01-01,
  2025-01-02,
  2025-01-03,
  ...
]
```

Data must always remain sorted by date in ascending order.

---

# Entity Requirements

## PDVHistoryEntity

All fields must match repository entity fields.

Implement Comparable:

```java
public class PDVHistoryEntity implements Comparable<PDVHistoryEntity>
```

Comparison:

```java
@Override
public int compareTo(PDVHistoryEntity other) {
    return this.getDate().compareTo(other.getDate());
}
```

Purpose:

* Binary search
* Ordered insertion
* Range retrieval

---

# Cache Abstraction

## Interface

```java
public interface PDVCacheService
```

Responsibilities:

* Retrieve data
* Populate cache
* Refresh cache
* Load cache
* Persist cache
* Hide implementation details

Business services must only use this interface.

---

# Future Provider Support

Current implementation:

```java
InMemoryPDVCacheService
```

Future implementations:

```java
ChroniclePDVCacheService
RedisPDVCacheService
HazelcastPDVCacheService
```

Since all business services depend on:

```java
PDVCacheService
```

provider replacement becomes configuration driven.

---

# Repository Method Replication

All methods currently exposed by:

```java
PDVRepository
```

must have equivalent methods exposed by:

```java
PDVCacheService
```

Example:

Repository:

```java
findByTicker(...)
findByTickerAndDateBetween(...)
findByDate(...)
findAll()
```

Cache Service:

```java
findByTicker(...)
findByTickerAndDateBetween(...)
findByDate(...)
findAll()
```

Business services should eventually stop using repository directly.

---

# Cache Loading Strategy

## Configuration Driven

```properties
cache.pdv.enabled=true
cache.pdv.preload-years=1.5
cache.pdv.chronicle.enabled=true
cache.pdv.snapshot-interval-minutes=60
```

No hardcoded date ranges.

---

## Startup Flow

### Step 1

Attempt Chronicle Map restore. If failed skip with proper logs but do not stop bootup

```text
Load cache snapshot from disk
```

---

### Step 2

Determine latest cached date.

Example:

```text
Chronicle latest date:
2026-08-01
```

---

### Step 3

Determine latest DB date.

Example:

```text
Database latest date:
2026-08-02
```

---

### Step 4

Load missing records.

Query:

```sql
SELECT *
FROM pdvt
WHERE date > :latestChronicleDate
ORDER BY date ASC
```

---

### Step 5

Append missing records.

---

### Step 6

Save updated snapshot.

---

# Full Initial Load

When Chronicle Map is missing:

```sql
SELECT *
FROM pdvt
WHERE date >= :configuredDate
ORDER BY ticker,date
```

Example:

```text
Today = 2026-08-02

Preload Years = 1.5

Load From:
2025-02-02
```

---

# Sorting

All lists must remain sorted.

Load:

```java
data.sort(
    Comparator.comparing(PDVHistoryEntity::getDate)
);
```

Store:

```java
cache.put(
    ticker,
    new ArrayList<>(data)
);
```

---

# Range Retrieval

Primary use case:

```java
findByTickerAndDateBetween(
    ticker,
    fromDate,
    toDate
)
```

Must not scan entire list.

Use binary search.

---

# Binary Search Utilities

Implement utility class:

```java
PDVCacheUtils
```

Methods:

```java
lowerBound(...)
upperBound(...)
```

---

## lowerBound

Returns:

```text
First index >= fromDate
```

Complexity:

```text
O(log n)
```

---

## upperBound

Returns:

```text
Last index <= toDate
```

Complexity:

```text
O(log n)
```

---

## Range Retrieval

```java
int start = lowerBound(...);

int end = upperBound(...);

return list.subList(start, end + 1);
```

Complexity:

```text
O(log n + k)
```

Where:

```text
k = returned records
```

---

# Cache Miss Handling

Scenario:

Ticker not present.

Flow:

```text
Read DB
Load Records
Sort
Insert Into Cache
Return Data
```

Example:

```java
cache.computeIfAbsent(
    ticker,
    this::loadTickerFromDb
);
```

---

# New Record Insertion

Daily incremental update.

Example:

```java
ArrayList<PDVHistoryEntity> list
```

Use binary search insertion.

```java
Collections.binarySearch(...)
```

Insert at correct position.

Maintain sorted order.

---

# Thread Safety

Primary cache:

```java
ConcurrentHashMap<
    String,
    ArrayList<PDVHistoryEntity>
>
```

Operations modifying ticker data must be synchronized per ticker.

Avoid global locks.

Recommended:

```java
compute(...)
computeIfAbsent(...)
```

---

# Chronicle Map Persistence

## Purpose

Avoid:

```text
DB Read
+
Object Mapping
+
Cache Reconstruction
```

during every startup.

---

## Snapshot File

Example:

```text
/data/cache/pdv-cache.dat
```

Docker volume mounted.

---

## Save Schedule

Every:

```text
300 Minutes
```

Configurable.

```properties
cache.pdv.snapshot-interval-minutes=300
```

---

## Snapshot Flow

```text
Current Cache
      |
Serialize
      |
Chronicle Map
      |
Disk
```

---

# Startup Recovery Flow

```text
Application Startup
        |
Load Chronicle Snapshot
        |
Latest Cached Date
        |
Query Missing Records
        |
Merge
        |
Cache Ready
```

---

# REST Endpoints

Base URL:

```text
/api/cache/pdv
```

---

## Refresh Cache

```http
POST /api/cache/pdv/reload
```

Action:

```text
Clear Cache
Read DB
Rebuild Cache
Save Chronicle
```

---

## Clear Cache

```http
POST /api/cache/pdv/clear
```

Action:

```text
Clear Memory Cache
```

---

## Clear Chronicle

```http
POST /api/cache/pdv/chronicle/clear
```

Action:

```text
Delete Chronicle Snapshot
```

---

## Reload Chronicle

```http
POST /api/cache/pdv/chronicle/reload
```

Action:

```text
Load Snapshot Into Memory
```

---

## Cache Stats

```http
GET /api/cache/pdv/stats
```

Response:

```json
{
  "tickers": 3000,
  "records": 1500000,
  "memoryUsedMB": 450,
  "chronicleEnabled": true,
  "lastSnapshotTime": "2026-08-02T10:00:00"
}
```

---

# Docker Requirements

Chronicle cache must survive container restart.

Mount volume:

```yaml
volumes:
  - ./cache:/data/cache
```

Environment:

```yaml
CACHE_DIRECTORY=/data/cache
```

Snapshot path:

```text
/data/cache/pdv-cache.dat
```

---

# Monitoring

Expose metrics:

```text
Cache Hit Count
Cache Miss Count
Cache Hit Ratio
Current Cache Size
Chronicle Save Time
Chronicle Load Time
DB Fallback Count
Range Query Count
```

Micrometer integration recommended.

---

# Recommended Additional Features

## Scheduled Incremental Refresh

Instead of full reloads:

```text
Every Night
```

Load only:

```sql
SELECT *
FROM pdvt
WHERE date > :lastLoadedDate
ORDER BY date ASC
```

Append into cache.

---

## Warm-Up Completion Flag

Application should not serve requests until:

```text
Cache Ready = TRUE
```

to avoid partial cache reads.

---

## Configurable Cache Provider

```properties
cache.provider=inmemory
```

Future:

```properties
cache.provider=chronicle
cache.provider=hazelcast
cache.provider=redis
```

Factory pattern should instantiate correct implementation.

---

# Final Design Summary

* Use `ConcurrentHashMap<String, ArrayList<PDVHistoryEntity>>`.
* Lists remain sorted by date ascending.
* Binary search (`lowerBound` / `upperBound`) used for date range retrieval.
* Cache layer mirrors repository methods.
* Business services depend only on `PDVCacheService`.
* Chronicle Map used for periodic snapshot persistence.
* Startup restores from Chronicle then loads missing DB records.
* Preload duration configurable through properties.
* Cache refresh, clear, chronicle reload, and stats endpoints exposed.
* Design supports future migration to Redis/Hazelcast/other cache providers without business service changes.
