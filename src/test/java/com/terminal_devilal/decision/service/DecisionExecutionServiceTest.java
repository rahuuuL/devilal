package com.terminal_devilal.decision.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.terminal_devilal.decision.api.DecisionRequests.EvaluationRequest;
import com.terminal_devilal.decision.api.DecisionRequests.SubjectRequest;
import com.terminal_devilal.decision.entity.DecisionProfileEntity;
import com.terminal_devilal.decision.indicator.IndicatorEvaluationContext;
import com.terminal_devilal.decision.indicator.IndicatorParameterResolver;
import com.terminal_devilal.decision.indicator.IndicatorProviderRegistry;
import com.terminal_devilal.decision.indicator.SubjectTickerResolver;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
import com.terminal_devilal.decision.repository.DecisionOutputVariableRepository;

class DecisionExecutionServiceTest {
	private final DecisionRuleService ruleService = mock(DecisionRuleService.class);
	private final DecisionOutputVariableRepository outputRepository = mock(DecisionOutputVariableRepository.class);
	private final DecisionIndicatorRepository indicatorRepository = mock(DecisionIndicatorRepository.class);
	private final DecisionRuleEvaluator evaluator = mock(DecisionRuleEvaluator.class);
	private final IndicatorProviderRegistry providerRegistry = mock(IndicatorProviderRegistry.class);
	private final IndicatorParameterResolver parameterResolver = mock(IndicatorParameterResolver.class);
	private final SubjectTickerResolver tickerResolver = mock(SubjectTickerResolver.class);
	private final DecisionExecutionService service = new DecisionExecutionService(ruleService, outputRepository,
			indicatorRepository, evaluator, providerRegistry, parameterResolver, tickerResolver);

	@Test
	void resolvesWatchlistSubjectsUsingTheWatchlistName() {
		LocalDate asOfDate = LocalDate.of(2026, 9, 27);
		DecisionProfileEntity profile = new DecisionProfileEntity(7L, "PROFILE", "Profile", null, "ACCUMULATE",
				"WATCHLIST");
		when(ruleService.profile(7L, "PROFILE")).thenReturn(profile);
		when(ruleService.activeDefinitions(7L, "PROFILE")).thenReturn(List.of());
		when(outputRepository.findByProfileIdOrderByCode(profile.getId())).thenReturn(List.of());
		when(evaluator.evaluateAndReturnFiredRules(any(), any(), any())).thenReturn(List.of());
		when(tickerResolver.resolve(any(IndicatorEvaluationContext.class))).thenReturn(List.of("AAA"));

		EvaluationRequest request = new EvaluationRequest(7L, "PROFILE", asOfDate, "WATCHLIST",
				List.of(new SubjectRequest("WATCHLIST", "my-watchlist", null, Map.of())));

		service.evaluate(request);

		ArgumentCaptor<IndicatorEvaluationContext> contextCaptor = ArgumentCaptor.forClass(IndicatorEvaluationContext.class);
		verify(tickerResolver).resolve(contextCaptor.capture());
		assertEquals("WATCHLIST", contextCaptor.getValue().getSubjectType());
		assertEquals("my-watchlist", contextCaptor.getValue().getSubjectId());
	}
}