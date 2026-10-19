package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoHistoryResponse;
import com.terminal_devilal.business_tools.ratio_analysis.service.SharpeSortinoHistoryService;
import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;

@Component
public class SharpeRatioSourceProvider implements IndicatorProvider {

    private static final Logger log = LoggerFactory.getLogger(SharpeRatioSourceProvider.class);
    private static final int DEFAULT_DAYS = 20;

    private final SharpeSortinoHistoryService historyService;
    private final DecisionIndicatorRepository indicatorRepository;
    private final ExpressionParser parser = new SpelExpressionParser();

    public SharpeRatioSourceProvider(SharpeSortinoHistoryService historyService,
            DecisionIndicatorRepository indicatorRepository) {
        this.historyService = historyService;
        this.indicatorRepository = indicatorRepository;
    }

    @Override
    public String getIndicatorCode() {
        return "SHARPE_RATIO_SOURCE";
    }

    @Override
    public Object getValue(IndicatorEvaluationContext context, Map<String, Object> parameters) {
        if (context == null || context.getAsOfDate() == null) {
            log.warn("SharpeRatioSourceProvider requires an evaluation context with asOfDate");
            return null;
        }
        if (context.getSubjectType() == null || context.getSubjectType().isBlank()) {
            log.warn("SharpeRatioSourceProvider requires a subjectType for subject {}", context.getSubjectId());
            return null;
        }

        String indicatorCode = stringParameter(parameters, "indicatorCode", "SORTINO");
        DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
        if (indicator == null || indicator.getFieldExpression() == null
                || indicator.getFieldExpression().isBlank()) {
            log.warn("SharpeRatioSourceProvider has no field expression configured for indicator {}", indicatorCode);
            return null;
        }

        LocalDate date = context.getAsOfDate();
        int days = resolveDays(parameters);
        List<String> tickers = resolveTickers(context, parameters);
        if (tickers.isEmpty()) {
            log.warn("SharpeRatioSourceProvider resolved no tickers for indicator {}", indicatorCode);
            return null;
        }

        List<SharpeSortinoHistoryResponse> rows = historyService.getHistory(date, date, days, tickers);
        log.debug("SharpeRatioSourceProvider fetched {} rows for indicator {} on {} with {} days and tickers {}",
                rows.size(), indicatorCode, date, days, tickers);

        Map<String, Object> valuesByTicker = new LinkedHashMap<>();
        for (SharpeSortinoHistoryResponse row : rows) {
            if (row == null || row.getTicker() == null || !date.equals(row.getDate())
                    || !Integer.valueOf(days).equals(row.getRollingDays())) {
                continue;
            }
            Object value = parser.parseExpression(indicator.getFieldExpression()).getValue(row);
            if (value != null) {
                valuesByTicker.put(row.getTicker(), value);
            }
        }

        if ("TICKER".equalsIgnoreCase(context.getSubjectType())) {
            Object value = valuesByTicker.get(tickers.get(0));
            log.info("SharpeRatioSourceProvider resolved indicator={} subject={} date={} days={} value={}",
                    indicatorCode, tickers.get(0), date, days, value);
            return value;
        }

        if ("MARKET".equalsIgnoreCase(context.getSubjectType())) {
            log.info("SharpeRatioSourceProvider resolved indicator={} date={} days={} tickerCount={} values={}",
                    indicatorCode, date, days, tickers.size(), valuesByTicker);
            return valuesByTicker;
        }

        throw new IllegalArgumentException("Unsupported subject type: " + context.getSubjectType());
    }

    private int resolveDays(Map<String, Object> parameters) {
        Object value = parameters == null ? null : parameters.get("days");
        if (value == null && parameters != null) {
            value = parameters.get("window");
        }
        if (value == null) {
            return DEFAULT_DAYS;
        }

        int days;
        if (value instanceof Number number) {
            double numericValue = number.doubleValue();
            if (!Double.isFinite(numericValue) || numericValue != Math.rint(numericValue)) {
                throw new IllegalArgumentException("days must be a whole number");
            }
            days = number.intValue();
        } else if (value instanceof String text) {
            days = Integer.parseInt(text);
        } else {
            throw new IllegalArgumentException("days must be numeric");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("days must be positive");
        }
        return days;
    }

    private List<String> resolveTickers(IndicatorEvaluationContext context, Map<String, Object> parameters) {
        if ("TICKER".equalsIgnoreCase(context.getSubjectType())) {
            if (context.getSubjectId() == null || context.getSubjectId().isBlank()) {
                log.warn("SharpeRatioSourceProvider received a TICKER context without a subjectId");
                return List.of();
            }
            return List.of(context.getSubjectId().trim());
        }
        if ("MARKET".equalsIgnoreCase(context.getSubjectType())) {
            Object tickerParameter = parameters == null ? null : parameters.get("tickers");
            if (!(tickerParameter instanceof Collection<?> collection)) {
                throw new IllegalArgumentException("MARKET Sharpe/Sortino provider requires parameters.tickers");
            }
            LinkedHashSet<String> tickers = new LinkedHashSet<>();
            for (Object value : collection) {
                if (value != null) {
                    String ticker = String.valueOf(value).trim();
                    if (!ticker.isBlank()) {
                        tickers.add(ticker);
                    }
                }
            }
            return List.copyOf(tickers);
        }
        throw new IllegalArgumentException("Unsupported subject type: " + context.getSubjectType());
    }

    private String stringParameter(Map<String, Object> parameters, String name, String defaultValue) {
        Object value = parameters == null ? null : parameters.get(name);
        return value == null ? defaultValue : String.valueOf(value);
    }
}
