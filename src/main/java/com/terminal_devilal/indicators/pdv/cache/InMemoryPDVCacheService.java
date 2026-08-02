package com.terminal_devilal.indicators.pdv.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.terminal_devilal.indicators.pdv.entity.PriceDeliveryVolumeEntity;
import com.terminal_devilal.indicators.pdv.entity.StockClosePrice;
import com.terminal_devilal.indicators.pdv.entity.projections.ClosePriceProjection;
import com.terminal_devilal.indicators.pdv.entity.projections.ConsistentVolumeProjection;
import com.terminal_devilal.indicators.pdv.entity.projections.PriceOhlcvProjection;
import com.terminal_devilal.indicators.pdv.repository.PriceDeliveryVolumeRepository;

import jakarta.annotation.PostConstruct;
import net.openhft.chronicle.map.ChronicleMap;

@Service
public class InMemoryPDVCacheService implements PDVCacheService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryPDVCacheService.class);

    private final PriceDeliveryVolumeRepository repository;
    private final PDVCacheProperties cacheProperties;

    private final ConcurrentHashMap<String, ArrayList<PriceDeliveryVolumeEntity>> cache = new ConcurrentHashMap<>();

    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong cacheMissCount = new AtomicLong(0);
    private final AtomicLong dbFallbackCount = new AtomicLong(0);
    private final AtomicLong rangeQueryCount = new AtomicLong(0);

    private volatile Instant lastSnapshotTime;
    private volatile long lastChronicleLoadMillis;
    private volatile long lastChronicleSaveMillis;
    private volatile LocalDate latestCachedDate;
    private volatile boolean cacheReady;
    private volatile boolean chronicleRuntimeAvailable = true;

    public InMemoryPDVCacheService(PriceDeliveryVolumeRepository repository, PDVCacheProperties cacheProperties) {
        this.repository = repository;
        this.cacheProperties = cacheProperties;
    }

    @PostConstruct
    public void initialize() {
        if (!cacheProperties.isEnabled()) {
            cacheReady = true;
            log.info("PDV cache is disabled via configuration");
            return;
        }

        long startedAt = System.currentTimeMillis();
        try {
            boolean restored = loadSnapshotFromChronicle();
            LocalDate maxDbDate = repository.findMaxDate();

            if (restored && latestCachedDate != null && maxDbDate != null && maxDbDate.isAfter(latestCachedDate)) {
                List<PriceDeliveryVolumeEntity> missing = repository.findByDateGreaterThanOrderByDateAsc(latestCachedDate);
                appendAll(missing);
                persistSnapshotInternal();
            } else if (!restored) {
                reloadCache();
            }
            cacheReady = true;
            log.info("PDV cache ready with {} tickers and {} records in {} ms", cache.size(), getRecordCount(),
                    System.currentTimeMillis() - startedAt);
        } catch (Exception e) {
            log.error("Failed to initialize PDV cache, continuing with DB fallback", e);
            cacheReady = false;
        }
    }

    @Override
    public List<PriceDeliveryVolumeEntity> findByTickerOrderByDateAsc(String ticker) {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.findByTickerOrderByDateAsc(ticker);
        }

        ArrayList<PriceDeliveryVolumeEntity> list = cache.computeIfAbsent(ticker, this::loadTickerFromDb);
        if (list.isEmpty()) {
            cacheMissCount.incrementAndGet();
            return List.of();
        }

        cacheHitCount.incrementAndGet();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    @Override
    public List<PriceDeliveryVolumeEntity> findByTickerAndDateBetweenOrderByDateAsc(String ticker, LocalDate fromDate,
            LocalDate toDate) {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.findByTickerAndDateBetweenOrderByDateAsc(ticker, fromDate, toDate);
        }

        rangeQueryCount.incrementAndGet();
        ArrayList<PriceDeliveryVolumeEntity> list = cache.computeIfAbsent(ticker, this::loadTickerFromDb);
        if (list.isEmpty()) {
            cacheMissCount.incrementAndGet();
            return List.of();
        }

        synchronized (list) {
            int start = PDVCacheUtils.lowerBound(list, fromDate);
            int end = PDVCacheUtils.upperBound(list, toDate);
            if (start > end || start >= list.size() || end < 0) {
                return List.of();
            }
            cacheHitCount.incrementAndGet();
            return new ArrayList<>(list.subList(start, end + 1));
        }
    }

    @Override
    public List<PriceDeliveryVolumeEntity> findByTickerAndDateGreaterThanEqualOrderByDateAsc(String ticker,
            LocalDate fromDate) {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate);
        }

        rangeQueryCount.incrementAndGet();
        ArrayList<PriceDeliveryVolumeEntity> list = cache.computeIfAbsent(ticker, this::loadTickerFromDb);
        if (list.isEmpty()) {
            cacheMissCount.incrementAndGet();
            return List.of();
        }

        synchronized (list) {
            int start = PDVCacheUtils.lowerBound(list, fromDate);
            if (start >= list.size()) {
                return List.of();
            }
            cacheHitCount.incrementAndGet();
            return new ArrayList<>(list.subList(start, list.size()));
        }
    }

    @Override
    public List<PriceDeliveryVolumeEntity> findByDate(LocalDate date) {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.findByDate(date);
        }

        List<PriceDeliveryVolumeEntity> result = new ArrayList<>();
        for (ArrayList<PriceDeliveryVolumeEntity> list : cache.values()) {
            synchronized (list) {
                int idx = PDVCacheUtils.lowerBound(list, date);
                if (idx < list.size() && list.get(idx).getDate().equals(date)) {
                    result.add(list.get(idx));
                }
            }
        }
        return result;
    }

    @Override
    public List<PriceDeliveryVolumeEntity> findAll() {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.findAll();
        }

        List<PriceDeliveryVolumeEntity> result = new ArrayList<>();
        for (ArrayList<PriceDeliveryVolumeEntity> list : cache.values()) {
            synchronized (list) {
                result.addAll(list);
            }
        }
        result.sort(Comparator.comparing(PriceDeliveryVolumeEntity::getTicker)
                .thenComparing(PriceDeliveryVolumeEntity::getDate));
        return result;
    }

    @Override
    public List<String> findDistinctTicker() {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.findDistinctTicker();
        }

        if (cache.isEmpty()) {
            cacheMissCount.incrementAndGet();
            return repository.findDistinctTicker();
        }
        return cache.keySet().stream().sorted().toList();
    }

    @Override
    public List<PriceOhlcvProjection> findByTickerInAndDateBetween(List<String> tickers, LocalDate fromDate,
            LocalDate toDate) {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.findByTickerInAndDateBetween(tickers, fromDate, toDate);
        }

        List<PriceOhlcvProjection> result = new ArrayList<>();
        for (String ticker : tickers) {
            List<PriceDeliveryVolumeEntity> rows = findByTickerAndDateBetweenOrderByDateAsc(ticker, fromDate, toDate);
            result.addAll(rows.stream().map(CachePriceOhlcvProjection::new).toList());
        }
        return result;
    }

    @Override
    public List<StockClosePrice> getClosePrices(LocalDate fromDate) {
        dbFallbackCount.incrementAndGet();
        return repository.getClosePrices(fromDate);
    }

    @Override
    public List<StockClosePrice> getClosePricesForStocks(LocalDate fromDate, List<String> tickers) {
        dbFallbackCount.incrementAndGet();
        return repository.getClosePricesForStocks(fromDate, tickers);
    }

    @Override
    public List<PriceOhlcvProjection> findLatestRecordForTickers(List<String> tickers) {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.findLatestRecordForTickers(tickers);
        }

        List<PriceOhlcvProjection> result = new ArrayList<>();
        for (String ticker : tickers) {
            ArrayList<PriceDeliveryVolumeEntity> list = cache.computeIfAbsent(ticker, this::loadTickerFromDb);
            if (list.isEmpty()) {
                continue;
            }
            synchronized (list) {
                result.add(new CachePriceOhlcvProjection(list.get(list.size() - 1)));
            }
        }
        return result;
    }

    @Override
    public Map<String, List<PriceDeliveryVolumeEntity>> getPDVForTickers(LocalDate fromDate, List<String> tickers) {
        return tickers.stream().collect(Collectors.toMap(ticker -> ticker,
                ticker -> findByTickerAndDateGreaterThanEqualOrderByDateAsc(ticker, fromDate)));
    }

    @Override
    public List<ClosePriceProjection> getAllCloseBetweenTwoDates(LocalDate fromDate, LocalDate toDate) {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.getAllCloseBetweenTwoDates(fromDate, toDate);
        }

        List<ClosePriceProjection> result = new ArrayList<>();
        for (Map.Entry<String, ArrayList<PriceDeliveryVolumeEntity>> entry : cache.entrySet()) {
            ArrayList<PriceDeliveryVolumeEntity> list = entry.getValue();
            synchronized (list) {
                int start = PDVCacheUtils.lowerBound(list, fromDate);
                int end = PDVCacheUtils.upperBound(list, toDate);
                if (start > end || start >= list.size() || end < 0) {
                    continue;
                }
                for (int i = start; i <= end; i++) {
                    PriceDeliveryVolumeEntity row = list.get(i);
                    result.add(new CacheClosePriceProjection(row.getTicker(), row.getDate(), row.getClose()));
                }
            }
        }
        result.sort(Comparator.comparing(ClosePriceProjection::getTicker).thenComparing(ClosePriceProjection::getDate));
        return result;
    }

    @Override
    public List<ClosePriceProjection> getAllCloseBetweenTwoDatesForTickers(List<String> tickers, LocalDate fromDate,
            LocalDate toDate) {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.getAllCloseBetweenTwoDatesForTickers(tickers, fromDate, toDate);
        }

        List<ClosePriceProjection> result = new ArrayList<>();
        for (String ticker : tickers) {
            List<PriceDeliveryVolumeEntity> rows = findByTickerAndDateBetweenOrderByDateAsc(ticker, fromDate, toDate);
            for (PriceDeliveryVolumeEntity row : rows) {
                result.add(new CacheClosePriceProjection(row.getTicker(), row.getDate(), row.getClose()));
            }
        }
        result.sort(Comparator.comparing(ClosePriceProjection::getTicker).thenComparing(ClosePriceProjection::getDate));
        return result;
    }

    @Override
    public List<ConsistentVolumeProjection> getAllVolumesBetweenTwoDates(LocalDate fromDate, LocalDate toDate) {
        if (!cacheProperties.isEnabled()) {
            dbFallbackCount.incrementAndGet();
            return repository.getAllVolumesBetweenTwoDates(fromDate, toDate);
        }

        List<ConsistentVolumeProjection> result = new ArrayList<>();
        for (Map.Entry<String, ArrayList<PriceDeliveryVolumeEntity>> entry : cache.entrySet()) {
            ArrayList<PriceDeliveryVolumeEntity> list = entry.getValue();
            synchronized (list) {
                int start = PDVCacheUtils.lowerBound(list, fromDate);
                int end = PDVCacheUtils.upperBound(list, toDate);
                if (start > end || start >= list.size() || end < 0) {
                    continue;
                }
                for (int i = start; i <= end; i++) {
                    PriceDeliveryVolumeEntity row = list.get(i);
                    result.add(new CacheConsistentVolumeProjection(row.getTicker(), row.getDate(), row.getVolume()));
                }
            }
        }
        result.sort(
                Comparator.comparing(ConsistentVolumeProjection::getTicker).thenComparing(ConsistentVolumeProjection::getDate));
        return result;
    }

    @Override
    public void reloadCache() {
        if (!cacheProperties.isEnabled()) {
            return;
        }

        cacheReady = false;
        clearCache();

        LocalDate loadFromDate = LocalDate.now()
                .minusDays(Math.max(1, Math.round(cacheProperties.getPreloadYears() * 365.0d)));

        List<PriceDeliveryVolumeEntity> rows = repository.findByDateGreaterThanEqualOrderByTickerAscDateAsc(loadFromDate);
        appendAll(rows);
        persistSnapshotInternal();
        cacheReady = true;

        log.info("Reloaded PDV cache from {} with {} records", loadFromDate, rows.size());
    }

    @Override
    public void clearCache() {
        cache.clear();
        latestCachedDate = null;
        cacheReady = false;
    }

    @Override
    public boolean clearChronicleSnapshot() {
        Path snapshot = resolveSnapshotPath();
        if (!Files.exists(snapshot)) {
            return false;
        }
        try {
            Files.delete(snapshot);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete PDV chronicle snapshot file {}", snapshot, e);
            return false;
        }
    }

    @Override
    public boolean reloadFromChronicleSnapshot() {
        clearCache();
        boolean loaded = loadSnapshotFromChronicle();
        cacheReady = loaded;
        return loaded;
    }

    @Override
    public void persistSnapshot() {
        persistSnapshotInternal();
    }

    @Scheduled(fixedDelayString = "${cache.pdv.snapshot-interval-ms:18000000}")
    public void scheduledSnapshot() {
        if (cacheProperties.isEnabled() && cacheProperties.isChronicleEnabled() && cacheReady) {
            persistSnapshotInternal();
        }
    }

    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        long records = getRecordCount();
        long heapUsedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        stats.put("enabled", cacheProperties.isEnabled());
        stats.put("ready", cacheReady);
        stats.put("tickers", cache.size());
        stats.put("records", records);
        stats.put("memoryUsedMB", heapUsedBytes / (1024.0d * 1024.0d));
        stats.put("chronicleEnabled", cacheProperties.isChronicleEnabled());
        stats.put("lastSnapshotTime", lastSnapshotTime == null ? null : lastSnapshotTime.toString());
        stats.put("latestCachedDate", latestCachedDate == null ? null : latestCachedDate.toString());
        stats.put("cacheHitCount", cacheHitCount.get());
        stats.put("cacheMissCount", cacheMissCount.get());
        stats.put("cacheHitRatio", calculateHitRatio());
        stats.put("dbFallbackCount", dbFallbackCount.get());
        stats.put("rangeQueryCount", rangeQueryCount.get());
        stats.put("chronicleLoadMillis", lastChronicleLoadMillis);
        stats.put("chronicleSaveMillis", lastChronicleSaveMillis);
        return stats;
    }

    private ArrayList<PriceDeliveryVolumeEntity> loadTickerFromDb(String ticker) {
        dbFallbackCount.incrementAndGet();
        List<PriceDeliveryVolumeEntity> fromDb = repository.findByTickerOrderByDateAsc(ticker);
        ArrayList<PriceDeliveryVolumeEntity> list = new ArrayList<>(fromDb);
        if (!list.isEmpty()) {
            updateLatestDate(list.get(list.size() - 1).getDate());
        }
        return list;
    }

    private void appendAll(List<PriceDeliveryVolumeEntity> rows) {
        for (PriceDeliveryVolumeEntity row : rows) {
            cache.compute(row.getTicker(), (ticker, current) -> {
                ArrayList<PriceDeliveryVolumeEntity> list = current == null ? new ArrayList<>() : current;
                synchronized (list) {
                    int idx = PDVCacheUtils.lowerBound(list, row.getDate());
                    if (idx < list.size() && list.get(idx).getDate().equals(row.getDate())) {
                        list.set(idx, row);
                    } else {
                        list.add(idx, row);
                    }
                }
                return list;
            });
            updateLatestDate(row.getDate());
        }
    }

    private boolean loadSnapshotFromChronicle() {
        if (!cacheProperties.isChronicleEnabled() || !chronicleRuntimeAvailable) {
            return false;
        }

        Path snapshot = resolveSnapshotPath();
        if (!Files.exists(snapshot)) {
            return false;
        }

        long startedAt = System.currentTimeMillis();
        try (ChronicleMap<String, byte[]> chronicle = ChronicleMap
                .of(String.class, byte[].class)
                .name("pdv-cache")
                .entries(Math.max(1024L, cacheProperties.getChronicleEntries()))
                .averageKey("RELIANCE|2026-08-01")
                .averageValue(new byte[512])
                .createPersistedTo(snapshot.toFile())) {

            for (Map.Entry<String, byte[]> entry : chronicle.entrySet()) {
                PriceDeliveryVolumeEntity row = deserialize(entry.getValue());
                if (row == null) {
                    continue;
                }
                cache.computeIfAbsent(row.getTicker(), key -> new ArrayList<>()).add(row);
                updateLatestDate(row.getDate());
            }

            for (ArrayList<PriceDeliveryVolumeEntity> list : cache.values()) {
                synchronized (list) {
                    list.sort(Comparator.comparing(PriceDeliveryVolumeEntity::getDate));
                }
            }

            lastChronicleLoadMillis = System.currentTimeMillis() - startedAt;
            log.info("Loaded PDV chronicle snapshot in {} ms", lastChronicleLoadMillis);
            return !cache.isEmpty();
        } catch (Throwable e) {
            disableChronicleRuntime(e);
            log.warn("Failed to load PDV chronicle snapshot, falling back to DB", e);
            return false;
        }
    }

    private void persistSnapshotInternal() {
        if (!cacheProperties.isEnabled() || !cacheProperties.isChronicleEnabled() || !chronicleRuntimeAvailable
                || cache.isEmpty()) {
            return;
        }

        long startedAt = System.currentTimeMillis();
        Path snapshot = resolveSnapshotPath();

        try {
            Files.createDirectories(snapshot.getParent());
            Files.deleteIfExists(snapshot);
        } catch (IOException e) {
            log.error("Failed preparing chronicle snapshot path {}", snapshot, e);
            return;
        }

        long entries = Math.max(1024L, getRecordCount() + 16L);

        try (ChronicleMap<String, byte[]> chronicle = ChronicleMap
                .of(String.class, byte[].class)
                .name("pdv-cache")
                .entries(entries)
                .averageKey("RELIANCE|2026-08-01")
                .averageValue(new byte[512])
                .createPersistedTo(snapshot.toFile())) {

            for (Map.Entry<String, ArrayList<PriceDeliveryVolumeEntity>> tickerEntry : cache.entrySet()) {
                ArrayList<PriceDeliveryVolumeEntity> list = tickerEntry.getValue();
                synchronized (list) {
                    for (PriceDeliveryVolumeEntity row : list) {
                        String key = buildChronicleKey(row);
                        chronicle.put(key, serialize(row));
                    }
                }
            }

            lastChronicleSaveMillis = System.currentTimeMillis() - startedAt;
            lastSnapshotTime = Instant.now();
            log.info("Saved PDV chronicle snapshot in {} ms", lastChronicleSaveMillis);
        } catch (Throwable e) {
            disableChronicleRuntime(e);
            log.error("Failed persisting PDV cache snapshot", e);
        }
    }

    private void disableChronicleRuntime(Throwable cause) {
        if (!chronicleRuntimeAvailable) {
            return;
        }

        chronicleRuntimeAvailable = false;
        log.error(
                "Disabling Chronicle PDV snapshot support due to runtime/module error. "
                        + "If you need Chronicle enabled on Java 17+, add JVM flag: "
                        + "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                cause);
    }

    private String buildChronicleKey(PriceDeliveryVolumeEntity row) {
        return row.getTicker() + "|" + row.getDate();
    }

    private byte[] serialize(PriceDeliveryVolumeEntity row) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(bos))) {
            out.writeUTF(row.getTicker());
            out.writeUTF(row.getDate().toString());
            out.writeDouble(row.getHigh());
            out.writeDouble(row.getLow());
            out.writeDouble(row.getOpen());
            out.writeDouble(row.getClose());
            out.writeDouble(row.getLastTradeValue());
            out.writeDouble(row.getPrevoiusClosePrice());
            out.writeLong(row.getVolume());
            out.writeDouble(row.getValue());
            out.writeInt(row.getTrades());
            out.writeLong(row.getDeliveryTrade());
            out.writeDouble(row.getDeliveryPercentage());
            out.writeDouble(row.getVwap());
        }
        return bos.toByteArray();
    }

    private PriceDeliveryVolumeEntity deserialize(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes)))) {
            PriceDeliveryVolumeEntity row = new PriceDeliveryVolumeEntity();
            row.setTicker(in.readUTF());
            row.setDate(LocalDate.parse(in.readUTF()));
            row.setHigh(in.readDouble());
            row.setLow(in.readDouble());
            row.setOpen(in.readDouble());
            row.setClose(in.readDouble());
            row.setLastTradeValue(in.readDouble());
            row.setPrevoiusClosePrice(in.readDouble());
            row.setVolume(in.readLong());
            row.setValue(in.readDouble());
            row.setTrades(in.readInt());
            row.setDeliveryTrade(in.readLong());
            row.setDeliveryPercentage(in.readDouble());
            row.setVwap(in.readDouble());
            return row;
        } catch (Exception e) {
            log.warn("Failed to deserialize PDV cache entry", e);
            return null;
        }
    }

    private Path resolveSnapshotPath() {
        return Paths.get(cacheProperties.getDirectory(), cacheProperties.getSnapshotFileName());
    }

    private long getRecordCount() {
        return cache.values().stream().mapToLong(List::size).sum();
    }

    private void updateLatestDate(LocalDate candidate) {
        if (candidate == null) {
            return;
        }

        LocalDate current = latestCachedDate;
        if (current == null || candidate.isAfter(current)) {
            latestCachedDate = candidate;
        }
    }

    private double calculateHitRatio() {
        long hits = cacheHitCount.get();
        long misses = cacheMissCount.get();
        long total = hits + misses;
        if (total == 0) {
            return 0.0d;
        }
        return ((double) hits / total) * 100.0d;
    }
}
