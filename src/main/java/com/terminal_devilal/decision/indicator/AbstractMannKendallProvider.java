package com.terminal_devilal.decision.indicator;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.terminal_devilal.business_tools.mannkendall.service.MannKendallHistoryService;
import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;

abstract class AbstractMannKendallProvider implements IndicatorProvider {
    private final MannKendallHistoryService historyService;

    protected AbstractMannKendallProvider(MannKendallHistoryService historyService) {
        this.historyService = historyService;
    }

    @Override
    public Object getValue(IndicatorEvaluationContext context, java.util.Map<String, Object> parameters) {
        if (!(context instanceof MannKendallIndicatorEvaluationContext mkContext)
                || mkContext.getSubjectId() == null) {
            return null;
        }

        LocalDate toDate = date(parameters.get("toDate"), mkContext.getToDate());
        LocalDate fromDate = date(parameters.get("fromDate"), mkContext.getFromDate());
        if (toDate == null) {
            return null;
        }
        if (fromDate == null) {
            fromDate = toDate;
        }
        Integer days = number(parameters.get("days"), mkContext.getDays());

        Set<String> tickers = Set.of(mkContext.getSubjectId());
        if (days == null) {
            return null;
        }
        List<MkResultHistoryEntity> records = historyService.fetchByDateRangeDaysAndTickers(
                fromDate, toDate, days, tickers);
        return records.stream().findFirst().map(this::value).orElse(null);
    }

    protected abstract Object value(MkResultHistoryEntity record);

    private LocalDate date(Object value, LocalDate fallback) {
        return value instanceof LocalDate localDate ? localDate
                : value instanceof String text ? LocalDate.parse(text) : fallback;
    }

    private Integer number(Object value, Integer fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
}