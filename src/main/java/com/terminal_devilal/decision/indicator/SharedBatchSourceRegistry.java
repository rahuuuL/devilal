package com.terminal_devilal.decision.indicator;

import com.terminal_devilal.indicators.volume.model.ConsistentVolumeSignalResponse;
import com.terminal_devilal.indicators.volume.service.ConsistentVolumeDetector;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SharedBatchSourceRegistry {
    private final ConsistentVolumeDetector detector;
    private final Map<String, List<?>> cache = new ConcurrentHashMap<>();

    public SharedBatchSourceRegistry(ConsistentVolumeDetector detector) {
        this.detector = detector;
    }

    public List<?> getOrCall(String sourceProviderCode, Map<String, Object> resolvedParams) {
        String key = sourceProviderCode + ":" + paramsKey(resolvedParams);
        return cache.computeIfAbsent(key, ignored -> callSource(sourceProviderCode, resolvedParams));
    }

    private List<?> callSource(String sourceProviderCode, Map<String, Object> resolvedParams) {
        if ("CONSISTENT_VOLUME_SOURCE".equals(sourceProviderCode)) {
            List<String> tickers = toTickerList(resolvedParams.get("tickers"));
            LocalDate fromDate = toLocalDate(resolvedParams.get("fromDate"), LocalDate.now().minusMonths(18));
            LocalDate toDate = toLocalDate(resolvedParams.get("toDate"), LocalDate.now());
            int baselineWindow = toInt(resolvedParams.get("baselineWindow"), 20);
            double baselineLowPercentile = toDouble(resolvedParams.get("baselineLowPercentile"), 20.0);
            double baselineHighPercentile = toDouble(resolvedParams.get("baselineHighPercentile"), 80.0);
            int rvolPercentileWindow = toInt(resolvedParams.get("rvolPercentileWindow"), 60);
            double rvolThresholdPercentile = toDouble(resolvedParams.get("rvolThresholdPercentile"), 75.0);
            int consistencyWindow = toInt(resolvedParams.get("consistencyWindow"), 10);
            int requiredScore = toInt(resolvedParams.get("requiredScore"), 7);
            return detector.detectConsistentVolumes(
                    tickers,
                    fromDate,
                    toDate,
                    baselineWindow,
                    baselineLowPercentile,
                    baselineHighPercentile,
                    rvolPercentileWindow,
                    rvolThresholdPercentile,
                    consistencyWindow,
                    requiredScore);
        }
        return List.of();
    }

    private String paramsKey(Map<String, Object> resolvedParams) {
        if (resolvedParams == null || resolvedParams.isEmpty()) {
            return "empty";
        }
        return resolvedParams.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + Objects.toString(entry.getValue()))
                .reduce((left, right) -> left + ";" + right)
                .orElse("empty");
    }

    private List<String> toTickerList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        if (value instanceof String text && !text.isBlank()) {
            return List.of(text.split(ConsistentVolumeDetector.TICKER_SEPARATOR));
        }
        return List.of();
    }

    private LocalDate toLocalDate(Object value, LocalDate fallback) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof String text && !text.isBlank()) return LocalDate.parse(text);
        return fallback;
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        return fallback;
    }

    private double toDouble(Object value, double fallback) {
        if (value instanceof Number number) return number.doubleValue();
        return fallback;
    }
}
