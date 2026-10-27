package com.terminal_devilal.decision.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.terminal_devilal.business_tools.mannkendall.service.MannKendallHistoryService;
import com.terminal_devilal.business_tools.mannkendall.entity.MkResultHistoryEntity;
import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;

class MannKendallSourceProviderTest {
	private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 9, 30);

	@Test
	void keepsReturningNullByDefault() {
		MannKendallSourceProvider provider = createProvider();

		Object value = provider.getValue(new IndicatorEvaluationContext("TICKER", "AAA", AS_OF_DATE),
				Map.of("indicatorCode", "MK_TREND", "days", 5));

		assertNull(value);
	}

	@Test
	void resolvesTrendFromHistoryForTicker() {
		MannKendallHistoryService historyService = mock(MannKendallHistoryService.class);
		MkResultHistoryEntity row = new MkResultHistoryEntity();
		row.setTicker("AAA");
		row.setDate(AS_OF_DATE);
		row.setDays(5);
		row.setTrend("decreasing");
		when(historyService.fetchByDateRangeDaysAndTickers(AS_OF_DATE, AS_OF_DATE, 5, java.util.Set.of("AAA")))
				.thenReturn(List.of(row));

		DecisionIndicatorEntity indicator = new DecisionIndicatorEntity("MK_TREND", "trend", "TREND", "TICKER",
				"STRING", "VALUE", "PROVIDER", "MK_SOURCE", null, null, "Mann-Kendall trend");
		indicator.setSourceProviderCode("MK_SOURCE");
		indicator.setFieldExpression("trend");
		DecisionIndicatorRepository indicatorRepository = mock(DecisionIndicatorRepository.class);
		when(indicatorRepository.findById("MK_TREND")).thenReturn(Optional.of(indicator));
		MannKendallSourceProvider provider = new MannKendallSourceProvider(historyService, indicatorRepository);

		Object value = provider.getValue(new IndicatorEvaluationContext("TICKER", "AAA", AS_OF_DATE),
				Map.of("indicatorCode", "MK_TREND", "days", 5));

		assertEquals("decreasing", value);
	}

	@Test
	void resolvesAvailableMarketRowsByTicker() {
		MannKendallHistoryService historyService = mock(MannKendallHistoryService.class);
		MkResultHistoryEntity row = new MkResultHistoryEntity();
		row.setTicker("AAA");
		row.setDate(AS_OF_DATE);
		row.setDays(5);
		row.setTrend("increasing");
		when(historyService.fetchByDateRangeDaysAndTickers(AS_OF_DATE, AS_OF_DATE, 5,
				java.util.Set.of("AAA", "BBB"))).thenReturn(List.of(row));

		DecisionIndicatorEntity indicator = new DecisionIndicatorEntity("MK_TREND", "trend", "TREND", "TICKER",
				"STRING", "VALUE", "PROVIDER", "MK_SOURCE", null, null, "Mann-Kendall trend");
		indicator.setSourceProviderCode("MK_SOURCE");
		indicator.setFieldExpression("trend");
		indicator.setRowAggregation("FIRST");
		DecisionIndicatorRepository indicatorRepository = mock(DecisionIndicatorRepository.class);
		when(indicatorRepository.findById("MK_TREND")).thenReturn(Optional.of(indicator));
		MannKendallSourceProvider provider = new MannKendallSourceProvider(historyService, indicatorRepository);

		Object value = provider.getValue(new IndicatorEvaluationContext("MARKET", "", AS_OF_DATE),
				Map.of("indicatorCode", "MK_TREND", "days", 5, "tickers", List.of("AAA", "BBB")));

		assertEquals(Map.of("AAA", "increasing"), value);
	}

	private MannKendallSourceProvider createProvider() {
		MannKendallHistoryService historyService = mock(MannKendallHistoryService.class);
		when(historyService.fetchByDateRangeDaysAndTickers(AS_OF_DATE, AS_OF_DATE, 5, java.util.Set.of("AAA", "BBB")))
				.thenReturn(List.of());
		when(historyService.fetchByDateRangeDaysAndTickers(AS_OF_DATE, AS_OF_DATE, 5, java.util.Set.of("AAA")))
				.thenReturn(List.of());

		DecisionIndicatorEntity indicator = new DecisionIndicatorEntity("MK_TREND", "trend", "TREND", "TICKER",
				"STRING", "VALUE", "PROVIDER", "MK_SOURCE", null, null, "Mann-Kendall trend");
		indicator.setSourceProviderCode("MK_SOURCE");
		indicator.setFieldExpression("trend");
		indicator.setRowAggregation("FIRST");
		DecisionIndicatorRepository indicatorRepository = mock(DecisionIndicatorRepository.class);
		when(indicatorRepository.findById("MK_TREND")).thenReturn(Optional.of(indicator));
		return new MannKendallSourceProvider(historyService, indicatorRepository);
	}
}
