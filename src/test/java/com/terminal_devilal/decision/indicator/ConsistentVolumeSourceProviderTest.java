package com.terminal_devilal.decision.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
import com.terminal_devilal.indicators.volume.service.ConsistentVolumeDetector;

class ConsistentVolumeSourceProviderTest {

    @Test
    void resolvesScoreFromResolvedProviderParameters() {
        ConsistentVolumeDetector detector = mock(ConsistentVolumeDetector.class);
        when(detector.detectConsistentVolumes(
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
        )).thenReturn(List.of());

        DecisionIndicatorRepository repository = mock(DecisionIndicatorRepository.class);
        DecisionIndicatorEntity indicator = new DecisionIndicatorEntity(
                "CONSISTENT_VOLUME_SCORE",
                "score",
                "VOLUME",
                "TICKER",
                "NUMBER",
                "VALUE",
                "PROVIDER",
                "CONSISTENT_VOLUME_SOURCE",
                null,
                null,
                "desc");
        indicator.setSourceProviderCode("CONSISTENT_VOLUME_SOURCE");
        indicator.setFieldExpression("consistencyScore");
        indicator.setRowAggregation("MAX");
        when(repository.findById("CONSISTENT_VOLUME_SCORE")).thenReturn(java.util.Optional.of(indicator));

        ConsistentVolumeSourceProvider provider = new ConsistentVolumeSourceProvider(repository, detector);
        IndicatorEvaluationContext context = new IndicatorEvaluationContext("TICKER", "RELIANCE", LocalDate.of(2026, 9, 8));

        Map<String, Object> params = Map.of(
                "indicatorCode", "CONSISTENT_VOLUME_SCORE",
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
        assertEquals(null, value);
    }
}
