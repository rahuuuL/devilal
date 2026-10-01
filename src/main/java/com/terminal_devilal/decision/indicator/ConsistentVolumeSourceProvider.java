package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
import com.terminal_devilal.indicators.volume.dto.ConsistentVolumeSignalResponse;
import com.terminal_devilal.indicators.volume.service.ConsistentVolumeDetector;

@Component
public class ConsistentVolumeSourceProvider implements IndicatorProvider {
	private static final Logger log = LoggerFactory.getLogger(ConsistentVolumeSourceProvider.class);
	private final DecisionIndicatorRepository indicatorRepository;
	private final ConsistentVolumeDetector detector;
	private final ExpressionParser parser = new SpelExpressionParser();

	@Autowired
	public ConsistentVolumeSourceProvider(DecisionIndicatorRepository indicatorRepository,
			ConsistentVolumeDetector detector) {
		this.indicatorRepository = indicatorRepository;
		this.detector = detector;
	}

	public ConsistentVolumeSourceProvider(DecisionIndicatorRepository indicatorRepository,
			ConsistentVolumeDetector detector, Object ignored) {
		this(indicatorRepository, detector);
	}

	@Override
	public String getIndicatorCode() {
		return "CONSISTENT_VOLUME_SOURCE";
	}

	@Override
	public Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters) {
		log.info(
				"ConsistentVolumeSourceProvider.getValue() called. Context: subjectType={}, subjectId={}, asOfDate={}",
				context != null ? context.getSubjectType() : "null", context != null ? context.getSubjectId() : "null",
				context != null ? context.getAsOfDate() : "null");

		if (context == null || context.getAsOfDate() == null) {
			log.warn("ConsistentVolumeSourceProvider: context or asOfDate is null, returning null");
			return null;
		}

		// Note: For MARKET type, subjectId can be null/empty - that's OK, we'll resolve
		// all market tickers
		if (context.getSubjectType() == null || context.getSubjectType().isBlank()) {
			log.warn("ConsistentVolumeSourceProvider: subjectType is null or empty, returning null");
			return null;
		}

		String indicatorCode = parameters == null || parameters.isEmpty() ? "CONSISTENT_VOLUME_SCORE"
				: (String) parameters.getOrDefault("indicatorCode", "CONSISTENT_VOLUME_SCORE");
		DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
		if (indicator == null || indicator.getSourceProviderCode() == null || indicator.getFieldExpression() == null) {
			log.warn(
					"ConsistentVolumeSourceProvider: Indicator {} not found or missing config (sourceProviderCode or fieldExpression)",
					indicatorCode);
			return null;
		}

		LocalDate fromDate = parameters != null && parameters.get("fromDate") instanceof LocalDate localDate ? localDate
				: context.getAsOfDate().minusMonths(18);
		LocalDate toDate = parameters != null && parameters.get("toDate") instanceof LocalDate localDate ? localDate
				: context.getAsOfDate();

		List<String> tickers;
		if ("TICKER".equalsIgnoreCase(context.getSubjectType())) {
			if (context.getSubjectId() == null || context.getSubjectId().isBlank()) {
				return null;
			}
			tickers = List.of(context.getSubjectId());
		} else if ("MARKET".equalsIgnoreCase(context.getSubjectType())) {
			Object tickerParameter = parameters == null ? null : parameters.get("tickers");
			if (!(tickerParameter instanceof Collection<?> collection)) {
				throw new IllegalArgumentException("MARKET consistent-volume provider requires parameters.tickers");
			}
			tickers = collection.stream().filter(Objects::nonNull).map(String::valueOf).toList();
		} else {
			throw new IllegalArgumentException("Unsupported subject type: " + context.getSubjectType());
		}
		log.info("ConsistentVolumeSourceProvider: Resolved tickers for indicator {}. Count: {}", indicatorCode,
				tickers.size());

		if (tickers.isEmpty()) {
			log.warn("ConsistentVolumeSourceProvider: No tickers resolved for indicator {}", indicatorCode);
			return null;
		}

		List<ConsistentVolumeSignalResponse> rows = detector.detectConsistentVolumes(tickers, fromDate, toDate,
				asInt(parameters != null ? parameters.get("baselineWindow") : null, 20),
				asDouble(parameters != null ? parameters.get("baselineLowPercentile") : null, 20.0),
				asDouble(parameters != null ? parameters.get("baselineHighPercentile") : null, 80.0),
				asInt(parameters != null ? parameters.get("rvolPercentileWindow") : null, 60),
				asDouble(parameters != null ? parameters.get("rvolThresholdPercentile") : null, 75.0),
				asInt(parameters != null ? parameters.get("consistencyWindow") : null, 10),
				asInt(parameters != null ? parameters.get("requiredScore") : null, 7));

		log.info("ConsistentVolumeSourceProvider: Detector returned {} rows for indicator {}", rows.size(),
				indicatorCode);

		String fieldExpression = indicator.getFieldExpression();
		List<RowFilterDiagnostic> rowDiagnostics = rows.stream().map(row -> {
			if (row == null) {
				return new RowFilterDiagnostic(null, null, null, null, null, false, false, "NULL_ROW");
			}
			boolean tickerMatched = tickers.contains(row.getTicker());
			boolean dateMatched = row.getDate() != null && row.getDate().isEqual(toDate);
			String reason = tickerMatched && dateMatched ? "QUALIFIED"
					: !tickerMatched && !dateMatched ? "TICKER_AND_DATE_MISMATCH"
							: !tickerMatched ? "TICKER_MISMATCH" : "DATE_MISMATCH";
			return new RowFilterDiagnostic(row.getTicker(), row.getDate(), row.getConsistencyScore(),
					row.getConsistencyWindow(), row.getRelativeVolumesCombinedAverage(), tickerMatched, dateMatched,
					reason);
		}).toList();
		List<ConsistentVolumeSignalResponse> subjectRows = rows.stream().filter(Objects::nonNull)
				.filter(row -> tickers.contains(row.getTicker()))
				.filter(row -> row.getDate() != null && row.getDate().isEqual(toDate)).toList();

		List<RowFilterDiagnostic> zydusLifeDiagnostics = rowDiagnostics.stream()
				.filter(diagnostic -> "ZYDUSLIFE".equalsIgnoreCase(diagnostic.ticker())).toList();
		long qualifiedZydusLifeRows = zydusLifeDiagnostics.stream()
				.filter(diagnostic -> diagnostic.tickerMatched() && diagnostic.dateMatched()).count();
		log.info(
				"ConsistentVolumeSourceProvider: Row filter for indicator {} targetTicker=ZYDUSLIFE requestedDate={} inputRows={} qualifiedRows={} rejectedRows={} decisions={}",
				indicatorCode, toDate, zydusLifeDiagnostics.size(), qualifiedZydusLifeRows,
				zydusLifeDiagnostics.size() - qualifiedZydusLifeRows, zydusLifeDiagnostics);

		if (subjectRows.isEmpty()) {
			log.warn("ConsistentVolumeSourceProvider: No subject rows found after filtering for indicator {}",
					indicatorCode);
			return null;
		}

		String aggregation = indicator.getRowAggregation() == null ? "SINGLE" : indicator.getRowAggregation();
		Map<String, List<Object>> valuesByTicker = new LinkedHashMap<>();
		for (ConsistentVolumeSignalResponse row : subjectRows) {
			Object value = parser.parseExpression(fieldExpression).getValue(row);
			if (value != null) {
				valuesByTicker.computeIfAbsent(row.getTicker(), ignored -> new ArrayList<>()).add(value);
			}
		}
		if ("TICKER".equalsIgnoreCase(context.getSubjectType())) {
			return aggregateValues(valuesByTicker.get(tickers.get(0)), aggregation);
		}
		Map<String, Object> result = new LinkedHashMap<>();
		valuesByTicker.forEach((ticker, values) -> {
			Object value = aggregateValues(values, aggregation);
			if (value != null) {
				result.put(ticker, value);
			}
		});
		return result;
	}

	public Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters, Object ignored) {
		return getValue(context, parameters);
	}

	private int asInt(Object value, int fallback) {
		if (value instanceof Number number)
			return number.intValue();
		return fallback;
	}

	private double asDouble(Object value, double fallback) {
		if (value instanceof Number number)
			return number.doubleValue();
		return fallback;
	}

	private Object aggregateValues(List<Object> values, String aggregation) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		return switch (aggregation.toUpperCase(Locale.ROOT)) {
		case "MAX" ->
			values.stream().map(this::toDoubleOrNull).filter(Objects::nonNull).max(Double::compareTo).orElse(null);
		case "MIN" ->
			values.stream().map(this::toDoubleOrNull).filter(Objects::nonNull).min(Double::compareTo).orElse(null);
		case "FIRST", "SINGLE" -> values.get(0);
		default -> values.get(0);
		};
	}

	private Double toDoubleOrNull(Object value) {
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		if (value instanceof String text) {
			return Double.parseDouble(text);
		}
		return null;
	}

	private record RowFilterDiagnostic(String ticker, LocalDate date, Integer consistencyScore,
			Integer consistencyWindow, Double relativeVolumesCombinedAverage, boolean tickerMatched, boolean dateMatched,
			String reason) {
	}
}
