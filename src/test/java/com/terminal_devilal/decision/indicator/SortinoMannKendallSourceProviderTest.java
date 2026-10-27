package com.terminal_devilal.decision.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.terminal_devilal.business_tools.mannkendall.dto.MannKendallResponse;
import com.terminal_devilal.business_tools.mannkendall.service.AnalyzeMannKendallForTicker;
import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;

class SortinoMannKendallSourceProviderTest {
	private static final LocalDate FROM_DATE = LocalDate.of(2026, 9, 1);
	private static final LocalDate TO_DATE = LocalDate.of(2026, 9, 30);

	@Test
	void delegatesToSortinoAnalysisAndReturnsConfiguredResponseField() {
		AnalyzeMannKendallForTicker analysisService = mock(AnalyzeMannKendallForTicker.class);
		MannKendallResponse response = new MannKendallResponse();
		response.setTicker("AAA");
		response.setScore(2.5d);
		when(analysisService.getSortinoMannKendallTrendAnalysis(FROM_DATE, TO_DATE, 5, List.of("AAA")))
				.thenReturn(List.of(response));

		SortinoMannKendallSourceProvider provider = createProvider(analysisService);
		Object value = provider.getValue(new IndicatorEvaluationContext("TICKER", "AAA", TO_DATE),
				Map.of("indicatorCode", "SORTINO_MK_SCORE", "days", 5, "fromDate", FROM_DATE, "toDate", TO_DATE));

		assertEquals(2.5d, value);
		verify(analysisService).getSortinoMannKendallTrendAnalysis(FROM_DATE, TO_DATE, 5, List.of("AAA"));
	}

	@Test
	void returnsMarketValuesByTicker() {
		AnalyzeMannKendallForTicker analysisService = mock(AnalyzeMannKendallForTicker.class);
		MannKendallResponse first = new MannKendallResponse();
		first.setTicker("AAA");
		first.setTrend("increasing");
		MannKendallResponse second = new MannKendallResponse();
		second.setTicker("BBB");
		second.setTrend("decreasing");
		List<String> tickers = List.of("AAA", "BBB");
		when(analysisService.getSortinoMannKendallTrendAnalysis(FROM_DATE, TO_DATE, 5, tickers))
				.thenReturn(List.of(first, second));

		SortinoMannKendallSourceProvider provider = createProvider(analysisService);
		Object value = provider.getValue(new IndicatorEvaluationContext("MARKET", "", TO_DATE),
				Map.of("indicatorCode", "SORTINO_MK_TREND", "days", 5, "fromDate", FROM_DATE, "toDate", TO_DATE,
						"tickers", tickers));

		assertEquals(Map.of("AAA", "increasing", "BBB", "decreasing"), value);
	}

	private SortinoMannKendallSourceProvider createProvider(AnalyzeMannKendallForTicker analysisService) {
		DecisionIndicatorRepository indicatorRepository = mock(DecisionIndicatorRepository.class);
		when(indicatorRepository.findById("SORTINO_MK_SCORE"))
				.thenReturn(Optional.of(indicator("SORTINO_MK_SCORE", "score")));
		when(indicatorRepository.findById("SORTINO_MK_TREND"))
				.thenReturn(Optional.of(indicator("SORTINO_MK_TREND", "trend")));
		return new SortinoMannKendallSourceProvider(analysisService, indicatorRepository);
	}

	private DecisionIndicatorEntity indicator(String code, String fieldExpression) {
		DecisionIndicatorEntity indicator = new DecisionIndicatorEntity(code, code, "TREND", "TICKER", "NUMBER",
				"VALUE", "PROVIDER", "MK_SORTINO_SOURCE", null, null, "Sortino Mann-Kendall " + fieldExpression);
		indicator.setSourceProviderCode("MK_SORTINO_SOURCE");
		indicator.setFieldExpression(fieldExpression);
		return indicator;
	}
}
