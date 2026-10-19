package com.terminal_devilal.decision.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoHistoryResponse;
import com.terminal_devilal.business_tools.ratio_analysis.service.SharpeSortinoHistoryService;
import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;

@ExtendWith(MockitoExtension.class)
class SharpeRatioSourceProviderTest {

    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 10, 19);

    @Mock
    private SharpeSortinoHistoryService historyService;

    @Mock
    private DecisionIndicatorRepository indicatorRepository;

    @InjectMocks
    private SharpeRatioSourceProvider provider;

    @Test
    void returnsSharpeRatioFromHistoryForTickerAndConfiguredWindow() {
        when(indicatorRepository.findById("SHARPE_RATIO"))
                .thenReturn(java.util.Optional.of(indicator("SHARPE_RATIO", "sharpeRatio")));
        when(historyService.getHistory(AS_OF_DATE, AS_OF_DATE, 50, List.of("AAA")))
                .thenReturn(List.of(historyRow("AAA", 50, 1.25, -0.4)));

        Object value = provider.getValue(new IndicatorEvaluationContext("TICKER", "AAA", AS_OF_DATE),
                Map.of("indicatorCode", "SHARPE_RATIO", "days", 50));

        assertEquals(1.25, value);
        verify(historyService).getHistory(AS_OF_DATE, AS_OF_DATE, 50, List.of("AAA"));
    }

    @Test
    void returnsSortinoRatioAndDefaultsToExistingTwentyDayWindow() {
        when(indicatorRepository.findById("SORTINO"))
                .thenReturn(java.util.Optional.of(indicator("SORTINO", "sortinoRatio")));
        when(historyService.getHistory(AS_OF_DATE, AS_OF_DATE, 20, List.of("AAA")))
                .thenReturn(List.of(historyRow("AAA", 20, 1.25, 0.8)));

        Object value = provider.getValue(new IndicatorEvaluationContext("TICKER", "AAA", AS_OF_DATE),
                Map.of("indicatorCode", "SORTINO"));

        assertEquals(0.8, value);
        verify(historyService).getHistory(AS_OF_DATE, AS_OF_DATE, 20, List.of("AAA"));
    }

    @Test
    void returnsNullWhenHistoryHasNoMatchingAsOfDate() {
        when(indicatorRepository.findById("SHARPE_RATIO"))
                .thenReturn(java.util.Optional.of(indicator("SHARPE_RATIO", "sharpeRatio")));
        when(historyService.getHistory(AS_OF_DATE, AS_OF_DATE, 20, List.of("AAA")))
                .thenReturn(List.of(historyRow("AAA", 20, 1.25, 0.8, AS_OF_DATE.minusDays(1))));

        Object value = provider.getValue(new IndicatorEvaluationContext("TICKER", "AAA", AS_OF_DATE),
                Map.of("indicatorCode", "SHARPE_RATIO"));

        assertNull(value);
    }

    private DecisionIndicatorEntity indicator(String code, String fieldExpression) {
        DecisionIndicatorEntity indicator = new DecisionIndicatorEntity(code, code, "RISK", "TICKER", "NUMBER",
                "RATIO", "PROVIDER", "SHARPE_RATIO_SOURCE", BigDecimal.ZERO, BigDecimal.TEN, code);
        indicator.setFieldExpression(fieldExpression);
        return indicator;
    }

    private SharpeSortinoHistoryResponse historyRow(String ticker, int days, double sharpe, double sortino) {
        return historyRow(ticker, days, sharpe, sortino, AS_OF_DATE);
    }

    private SharpeSortinoHistoryResponse historyRow(String ticker, int days, double sharpe, double sortino,
            LocalDate date) {
        SharpeSortinoHistoryResponse row = new SharpeSortinoHistoryResponse();
        row.setTicker(ticker);
        row.setDate(date);
        row.setRollingDays(days);
        row.setSharpeRatio(sharpe);
        row.setSortinoRatio(sortino);
        return row;
    }
}
