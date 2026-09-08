package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.terminal_devilal.indicators.volume.service.ConsistentVolumeDetector;

@Component
public class ConsistentVolumeScoreProvider implements IndicatorProvider {
    private static final Logger log = LoggerFactory.getLogger(ConsistentVolumeScoreProvider.class);

    private final ConsistentVolumeDetector detector;

    public ConsistentVolumeScoreProvider(ConsistentVolumeDetector detector) {
        this.detector = detector;
    }

    @Override
    public String getIndicatorCode() {
        return "CONSISTENT_VOLUME_SCORE";
    }

    @Override
    public Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters) {
        if (!(context instanceof VolumeIndicatorEvaluationContext volumeContext)
                || volumeContext.getSubjectId() == null || volumeContext.getAsOfDate() == null) {
            log.warn("Cannot compute CONSISTENT_VOLUME_SCORE because context is incomplete: context={}, subjectId={}, asOfDate={}",
                context, context != null ? context.getSubjectId() : null,
                context instanceof VolumeIndicatorEvaluationContext volume
                    ? volume.getAsOfDate() : null);
            return null;
        }

        log.info("Computing indicator CONSISTENT_VOLUME_SCORE for subjectType={} subjectId={} asOfDate={}",
            volumeContext.getSubjectType(), volumeContext.getSubjectId(), volumeContext.getAsOfDate());

        int lookbackMonths = volumeContext.getLookbackMonths() != null
            ? volumeContext.getLookbackMonths()
                : parameters.get("lookbackMonths") != null
                    ? ((Number) parameters.get("lookbackMonths")).intValue()
                    : 18;
        log.debug("lookbackMonths resolved to {} from context={} map={} default={}",
            lookbackMonths,
            volumeContext.getLookbackMonths(),
            parameters != null ? parameters.get("lookbackMonths") : null,
            18);

        LocalDate fromDate = volumeContext.getFromDate() != null
            ? volumeContext.getFromDate()
                : parameters.get("fromDate") != null
                    ? (LocalDate) parameters.get("fromDate")
                    : volumeContext.getAsOfDate().minusMonths(lookbackMonths);
        log.debug("fromDate resolved to {} from context={} map={} default={}",
            fromDate,
            volumeContext.getFromDate(),
            parameters != null ? parameters.get("fromDate") : null,
                volumeContext.getAsOfDate().minusMonths(lookbackMonths));

        LocalDate toDate = volumeContext.getToDate() != null
            ? volumeContext.getToDate()
                : parameters.get("toDate") != null
                    ? (LocalDate) parameters.get("toDate")
                    : volumeContext.getAsOfDate();
        log.debug("toDate resolved to {} from context={} map={} default={}",
            toDate,
            volumeContext.getToDate(),
            parameters != null ? parameters.get("toDate") : null,
                volumeContext.getAsOfDate());

        int baselineWindow = volumeContext.getBaselineWindow() != null
            ? volumeContext.getBaselineWindow()
                : parameters.get("baselineWindow") != null
                    ? ((Number) parameters.get("baselineWindow")).intValue()
                    : 20;
        log.debug("baselineWindow resolved to {}", baselineWindow);

        double baselineLowPercentile = volumeContext.getBaselineLowPercentile() != null
            ? volumeContext.getBaselineLowPercentile()
                : parameters.get("baselineLowPercentile") != null
                    ? ((Number) parameters.get("baselineLowPercentile")).doubleValue()
                    : 20.0;
        log.debug("baselineLowPercentile resolved to {}", baselineLowPercentile);

        double baselineHighPercentile = volumeContext.getBaselineHighPercentile() != null
            ? volumeContext.getBaselineHighPercentile()
                : parameters.get("baselineHighPercentile") != null
                    ? ((Number) parameters.get("baselineHighPercentile")).doubleValue()
                    : 80.0;
        log.debug("baselineHighPercentile resolved to {}", baselineHighPercentile);

        int rvolPercentileWindow = volumeContext.getRvolPercentileWindow() != null
            ? volumeContext.getRvolPercentileWindow()
                : parameters.get("rvolPercentileWindow") != null
                    ? ((Number) parameters.get("rvolPercentileWindow")).intValue()
                    : 60;
        log.debug("rvolPercentileWindow resolved to {}", rvolPercentileWindow);

        double rvolThresholdPercentile = volumeContext.getRvolThresholdPercentile() != null
            ? volumeContext.getRvolThresholdPercentile()
                : parameters.get("rvolThresholdPercentile") != null
                    ? ((Number) parameters.get("rvolThresholdPercentile")).doubleValue()
                    : 75.0;
        log.debug("rvolThresholdPercentile resolved to {}", rvolThresholdPercentile);

        int consistencyWindow = volumeContext.getConsistencyWindow() != null
            ? volumeContext.getConsistencyWindow()
                : parameters.get("consistencyWindow") != null
                    ? ((Number) parameters.get("consistencyWindow")).intValue()
                    : 10;
        log.debug("consistencyWindow resolved to {}", consistencyWindow);

        int requiredScore = volumeContext.getRequiredScore() != null
            ? volumeContext.getRequiredScore()
                : parameters.get("requiredScore") != null
                    ? ((Number) parameters.get("requiredScore")).intValue()
                    : 7;
        log.debug("requiredScore resolved to {}", requiredScore);

        List<String> tickers = Arrays.stream(volumeContext.getSubjectId().split(ConsistentVolumeDetector.TICKER_SEPARATOR))
            .map(String::trim)
            .filter(ticker -> !ticker.isEmpty())
            .toList();
        Object score = detector.computeScoreForTicker(
            tickers,
                fromDate,
                toDate,
                baselineWindow,
                baselineLowPercentile,
                baselineHighPercentile,
                rvolPercentileWindow,
                rvolThresholdPercentile,
                consistencyWindow,
                requiredScore
        );

            log.info("Indicator CONSISTENT_VOLUME_SCORE computed for subjectId={} fromDate={} toDate={} result={} using baselineWindow={}, baselineLowPercentile={}, baselineHighPercentile={}, rvolPercentileWindow={}, rvolThresholdPercentile={}, consistencyWindow={}, requiredScore={}",
                volumeContext.getSubjectId(), fromDate, toDate, score,
                baselineWindow, baselineLowPercentile, baselineHighPercentile,
                rvolPercentileWindow, rvolThresholdPercentile, consistencyWindow, requiredScore);
            return score;
    }
}
