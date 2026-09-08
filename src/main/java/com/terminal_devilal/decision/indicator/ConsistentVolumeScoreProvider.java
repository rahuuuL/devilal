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
        if (context == null || context.getSubjectId() == null || context.getAsOfDate() == null) {
            log.warn("Cannot compute CONSISTENT_VOLUME_SCORE because context is incomplete: context={}, subjectId={}, asOfDate={}",
                context, context != null ? context.getSubjectId() : null, context != null ? context.getAsOfDate() : null);
            return null;
        }

        log.info("Computing indicator CONSISTENT_VOLUME_SCORE for subjectType={} subjectId={} asOfDate={}",
            context.getSubjectType(), context.getSubjectId(), context.getAsOfDate());

        int lookbackMonths = context.getLookbackMonths() != null
                ? context.getLookbackMonths()
                : parameters.get("lookbackMonths") != null
                    ? ((Number) parameters.get("lookbackMonths")).intValue()
                    : 18;
        log.debug("lookbackMonths resolved to {} from context={} map={} default={}",
            lookbackMonths,
            context.getLookbackMonths(),
            parameters != null ? parameters.get("lookbackMonths") : null,
            18);

        LocalDate fromDate = context.getFromDate() != null
                ? context.getFromDate()
                : parameters.get("fromDate") != null
                    ? (LocalDate) parameters.get("fromDate")
                    : context.getAsOfDate().minusMonths(lookbackMonths);
        log.debug("fromDate resolved to {} from context={} map={} default={}",
            fromDate,
            context.getFromDate(),
            parameters != null ? parameters.get("fromDate") : null,
            context.getAsOfDate().minusMonths(lookbackMonths));

        LocalDate toDate = context.getToDate() != null
                ? context.getToDate()
                : parameters.get("toDate") != null
                    ? (LocalDate) parameters.get("toDate")
                    : context.getAsOfDate();
        log.debug("toDate resolved to {} from context={} map={} default={}",
            toDate,
            context.getToDate(),
            parameters != null ? parameters.get("toDate") : null,
            context.getAsOfDate());

        int baselineWindow = context.getBaselineWindow() != null
                ? context.getBaselineWindow()
                : parameters.get("baselineWindow") != null
                    ? ((Number) parameters.get("baselineWindow")).intValue()
                    : 20;
        log.debug("baselineWindow resolved to {}", baselineWindow);

        double baselineLowPercentile = context.getBaselineLowPercentile() != null
                ? context.getBaselineLowPercentile()
                : parameters.get("baselineLowPercentile") != null
                    ? ((Number) parameters.get("baselineLowPercentile")).doubleValue()
                    : 20.0;
        log.debug("baselineLowPercentile resolved to {}", baselineLowPercentile);

        double baselineHighPercentile = context.getBaselineHighPercentile() != null
                ? context.getBaselineHighPercentile()
                : parameters.get("baselineHighPercentile") != null
                    ? ((Number) parameters.get("baselineHighPercentile")).doubleValue()
                    : 80.0;
        log.debug("baselineHighPercentile resolved to {}", baselineHighPercentile);

        int rvolPercentileWindow = context.getRvolPercentileWindow() != null
                ? context.getRvolPercentileWindow()
                : parameters.get("rvolPercentileWindow") != null
                    ? ((Number) parameters.get("rvolPercentileWindow")).intValue()
                    : 60;
        log.debug("rvolPercentileWindow resolved to {}", rvolPercentileWindow);

        double rvolThresholdPercentile = context.getRvolThresholdPercentile() != null
                ? context.getRvolThresholdPercentile()
                : parameters.get("rvolThresholdPercentile") != null
                    ? ((Number) parameters.get("rvolThresholdPercentile")).doubleValue()
                    : 75.0;
        log.debug("rvolThresholdPercentile resolved to {}", rvolThresholdPercentile);

        int consistencyWindow = context.getConsistencyWindow() != null
                ? context.getConsistencyWindow()
                : parameters.get("consistencyWindow") != null
                    ? ((Number) parameters.get("consistencyWindow")).intValue()
                    : 10;
        log.debug("consistencyWindow resolved to {}", consistencyWindow);

        int requiredScore = context.getRequiredScore() != null
                ? context.getRequiredScore()
                : parameters.get("requiredScore") != null
                    ? ((Number) parameters.get("requiredScore")).intValue()
                    : 7;
        log.debug("requiredScore resolved to {}", requiredScore);

        List<String> tickers = Arrays.stream(context.getSubjectId().split(ConsistentVolumeDetector.TICKER_SEPARATOR))
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
                context.getSubjectId(), fromDate, toDate, score,
                baselineWindow, baselineLowPercentile, baselineHighPercentile,
                rvolPercentileWindow, rvolThresholdPercentile, consistencyWindow, requiredScore);
            return score;
    }
}
