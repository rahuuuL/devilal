package com.terminal_devilal.decision.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.terminal_devilal.indicators.volume.model.ConsistentVolumeSignalResponse;
import com.terminal_devilal.indicators.volume.service.ConsistentVolumeDetector;

class ConsistentVolumeScoreProviderTest {

    @Test
    void resolvesScoreFromProviderParameters() {
        ConsistentVolumeDetector detector = mock(ConsistentVolumeDetector.class);
        when(detector.computeScoreForTicker(
            List.of("RELIANCE"),
                LocalDate.of(2025, 3, 8),
                LocalDate.of(2026, 9, 8),
                20,
                20.0,
                80.0,
                60,
                75.0,
                10,
                7
        )).thenReturn(8.0);

        ConsistentVolumeScoreProvider provider = new ConsistentVolumeScoreProvider(detector);
        IndicatorEvaluationContext context = new VolumeIndicatorEvaluationContext("TICKER", "RELIANCE", LocalDate.of(2026, 9, 8));

        Map<String, Object> params = Map.of(
                "fromDate", LocalDate.of(2025, 3, 8),
                "toDate", LocalDate.of(2026, 9, 8),
                "baselineWindow", 20,
                "baselineLowPercentile", 20.0,
                "baselineHighPercentile", 80.0,
                "rvolPercentileWindow", 60,
                "rvolThresholdPercentile", 75.0,
                "consistencyWindow", 10,
                "requiredScore", 7
        );

        Object value = provider.getValue(context, params);

        assertEquals(8.0, ((Number) value).doubleValue());
    }
}
