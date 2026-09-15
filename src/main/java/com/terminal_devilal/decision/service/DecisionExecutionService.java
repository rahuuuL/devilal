package com.terminal_devilal.decision.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.terminal_devilal.decision.api.DecisionRequests.EvaluationRequest;
import com.terminal_devilal.decision.api.DecisionRequests.SubjectRequest;
import com.terminal_devilal.decision.api.DecisionResponses.EvaluationResponse;
import com.terminal_devilal.decision.api.DecisionResponses.SubjectResult;
import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.entity.DecisionOutputVariableEntity;
import com.terminal_devilal.decision.entity.DecisionProfileEntity;
import com.terminal_devilal.decision.indicator.IndicatorEvaluationContext;
import com.terminal_devilal.decision.indicator.IndicatorParameterResolver;
import com.terminal_devilal.decision.indicator.IndicatorProvider;
import com.terminal_devilal.decision.indicator.IndicatorProviderRegistry;
import com.terminal_devilal.decision.indicator.SubjectTickerResolver;
import com.terminal_devilal.decision.model.DecisionState;
import com.terminal_devilal.decision.model.RuleDefinition;
import com.terminal_devilal.decision.model.SubjectContext;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
import com.terminal_devilal.decision.repository.DecisionOutputVariableRepository;

@Service
public class DecisionExecutionService {
	private static final Logger log = LoggerFactory.getLogger(DecisionExecutionService.class);

	private final DecisionRuleService ruleService;
	private final DecisionOutputVariableRepository outputRepository;
	private final DecisionIndicatorRepository indicatorRepository;
	private final DecisionRuleEvaluator evaluator;
	private final IndicatorProviderRegistry providerRegistry;
	private final IndicatorParameterResolver parameterResolver;
	private final SubjectTickerResolver tickerResolver;

	public DecisionExecutionService(DecisionRuleService ruleService, DecisionOutputVariableRepository outputRepository,
			DecisionIndicatorRepository indicatorRepository, DecisionRuleEvaluator evaluator,
			IndicatorProviderRegistry providerRegistry, IndicatorParameterResolver parameterResolver,
			SubjectTickerResolver tickerResolver) {
		this.ruleService = ruleService;
		this.outputRepository = outputRepository;
		this.indicatorRepository = indicatorRepository;
		this.evaluator = evaluator;
		this.providerRegistry = providerRegistry;
		this.parameterResolver = parameterResolver;
		this.tickerResolver = tickerResolver;
	}

	public EvaluationResponse evaluate(EvaluationRequest request) {
		log.info("Starting evaluation: ownerId={}, profileCode={}, subjectCount={}, asOfDate={}", request.ownerId(),
				request.profileCode(), request.subjects() == null ? 0 : request.subjects().size(), request.asOfDate());
		DecisionProfileEntity profile = ruleService.profile(request.ownerId(), request.profileCode());
		if (!profile.getSubjectType().equals(request.subjectType()))
			throw new IllegalArgumentException("Subject type does not match profile");
		log.info("Loaded profile {} for subjectType={} with evaluationMode={}", profile.getCode(),
				profile.getSubjectType(), profile.getEvaluationMode());
		List<RuleDefinition> rules = ruleService.activeDefinitions(request.ownerId(), request.profileCode());
		log.info("Loaded {} active rules for profile {}", rules.size(), profile.getCode());
		List<SubjectResult> results;
		if ("MARKET".equalsIgnoreCase(request.subjectType())) {
			results = evaluateMarket(profile, rules, request);
		} else {
			results = request.subjects().stream()
					.map(subject -> evaluateOne(profile, rules, subject, request.asOfDate())).toList();
		}
		if ("FILTER".equals(profile.getEvaluationMode()))
			results = results.stream().filter(this::matched).toList();
		log.info("Evaluation finished for profile {}. Final result count: {}", profile.getCode(), results.size());
		return new EvaluationResponse(profile.getCode(), request.asOfDate(), results);
	}

	private List<SubjectResult> evaluateMarket(DecisionProfileEntity profile, List<RuleDefinition> rules,
			EvaluationRequest request) {
		LocalDate asOfDate = request.asOfDate();
		SubjectRequest marketSubject = request.subjects().stream().findFirst()
				.orElseThrow(() -> new IllegalArgumentException("MARKET request must contain a subject"));

		SubjectContext marketContext = new SubjectContext(marketSubject.subjectType(), marketSubject.subjectId(),
				asOfDate, marketSubject.attributes());
		IndicatorEvaluationContext evaluationContext = createEvaluationContext("MARKET", marketContext);
		List<String> tickers = tickerResolver.resolve(evaluationContext);
		log.info("MARKET resolved to {} tickers", tickers.size());
		if (tickers.isEmpty()) {
			return List.of();
		}

		Map<String, Map<String, Object>> valuesByTicker = resolveMarketProviderValues(rules, profile, asOfDate,
				tickers);
		List<SubjectResult> results = new ArrayList<>(tickers.size());
		for (String ticker : tickers) {
			SubjectRequest tickerRequest = new SubjectRequest("TICKER", ticker, asOfDate,
					valuesByTicker.getOrDefault(ticker, Collections.emptyMap()));
			results.add(evaluateOne(profile, rules, tickerRequest, asOfDate, true));
		}
		return results;
	}

	private SubjectResult evaluateOne(DecisionProfileEntity profile, List<RuleDefinition> rules, SubjectRequest request,
			LocalDate requestDate) {
		return evaluateOne(profile, rules, request, requestDate, false);
	}

	private SubjectResult evaluateOne(DecisionProfileEntity profile, List<RuleDefinition> rules, SubjectRequest request,
			LocalDate requestDate, boolean providerValuesAlreadyResolved) {
		LocalDate date = request.asOfDate() == null ? requestDate : request.asOfDate();
		log.info("Evaluating subject subjectType={}, subjectId={}, asOfDate={}", request.subjectType(),
				request.subjectId(), date);
		SubjectContext baseContext = new SubjectContext(request.subjectType(), request.subjectId(), date,
				request.attributes());
		log.debug("Base subject context attributes: {}", baseContext.getAttributes());
		SubjectContext context = providerValuesAlreadyResolved ? baseContext
				: resolveProviderValues(rules, baseContext);
		DecisionState state = createInitialState(profile);
		log.info("Resolved context attributes before evaluation: {}", context.getAttributes());
		List<String> fired = evaluator.evaluateAndReturnFiredRules(rules, context, state);
		log.info("Rules fired for subject {} {}: {}", request.subjectType(), request.subjectId(), fired);
		log.info("Final outputs for subject {} {}: {}", request.subjectType(), request.subjectId(), state.getOutputs());
		return new SubjectResult(request.subjectType(), request.subjectId(), date, baseContext.getAttributes(),
				context.getAttributes(), state.getOutputs(), fired);
	}

	private DecisionState createInitialState(DecisionProfileEntity profile) {
		Map<String, Object> initial = new LinkedHashMap<>();
		for (DecisionOutputVariableEntity output : outputRepository.findByProfileIdOrderByCode(profile.getId())) {
			initial.put(output.getCode(), parse(output.getInitialValue(), output.getValueType()));
		}
		return new DecisionState(initial);
	}

	private SubjectContext resolveProviderValues(List<RuleDefinition> rules, SubjectContext context) {
		Map<String, Object> merged = new LinkedHashMap<>(context.getAttributes());
		log.debug("Starting provider-based resolution for {} {} with {} rule conditions", context.getSubjectType(),
				context.getSubjectId(), rules.size());
		for (RuleDefinition rule : rules) {
			for (RuleDefinition.Condition condition : rule.conditions()) {
				String indicatorCode = condition.indicator();
				if (merged.containsKey(indicatorCode)) {
					log.debug(
							"Indicator {} already present in request attributes; skipping provider resolution. Value={}",
							indicatorCode, merged.get(indicatorCode));
					continue;
				}
				try {
					DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
					String providerCode = indicator != null && indicator.getSourceProviderCode() != null
							? indicator.getSourceProviderCode()
							: indicatorCode;
					IndicatorProvider provider = providerRegistry.get(providerCode);
					log.debug("Resolving indicator {} via provider {}", indicatorCode,
							provider.getClass().getSimpleName());
					IndicatorEvaluationContext evaluationContext = createEvaluationContext(context.getSubjectType(),
							context);
					Map<String, Object> parameters = parameterResolver.resolve(indicatorCode, condition,
							evaluationContext);
					parameters.putIfAbsent("indicatorCode", indicatorCode);
					parameters.putIfAbsent("sourceProviderCode", providerCode);
					if (indicator != null) {
						if (indicator.getFieldExpression() != null)
							parameters.putIfAbsent("fieldExpression", indicator.getFieldExpression());
						if (indicator.getRowFilterExpression() != null)
							parameters.putIfAbsent("rowFilterExpression", indicator.getRowFilterExpression());
						if (indicator.getRowAggregation() != null)
							parameters.putIfAbsent("rowAggregation", indicator.getRowAggregation());
					}
					log.debug("Resolved provider parameters for {}: {}", indicatorCode, parameters);
					Object value = provider.getValue(evaluationContext, parameters);
					if (value != null) {
						merged.put(indicatorCode, value);
						log.info("Indicator {} resolved from provider {} with value {}", indicatorCode,
								provider.getClass().getSimpleName(), value);
					} else {
						log.warn("Indicator {} resolved to null from provider {}", indicatorCode,
								provider.getClass().getSimpleName());
					}
				} catch (Exception e) {
					log.warn("Unable to resolve indicator {} from provider during rule execution. Reason: {}",
							indicatorCode, e.getMessage(), e);
					// Ignore missing provider or unsupported indicator; evaluator will later decide
					// if condition can be evaluated.
				}
			}
		}
		return new SubjectContext(context.getSubjectType(), context.getSubjectId(), context.getAsOfDate(), merged);
	}

	private Map<String, Map<String, Object>> resolveMarketProviderValues(List<RuleDefinition> rules,
			DecisionProfileEntity profile, LocalDate asOfDate, List<String> tickers) {
		Map<String, Map<String, Object>> valuesByTicker = new LinkedHashMap<>();
		for (String ticker : tickers) {
			valuesByTicker.put(ticker, new LinkedHashMap<>());
		}

		Set<String> resolvedIndicators = new HashSet<>();
		SubjectContext marketContext = new SubjectContext("MARKET", "", asOfDate, Collections.emptyMap());
		IndicatorEvaluationContext evaluationContext = createEvaluationContext("MARKET", marketContext);
		for (RuleDefinition rule : rules) {
			for (RuleDefinition.Condition condition : rule.conditions()) {
				String indicatorCode = condition.indicator();
				if (!resolvedIndicators.add(indicatorCode)) {
					continue;
				}
				try {
					DecisionIndicatorEntity indicator = indicatorRepository.findById(indicatorCode).orElse(null);
					String providerCode = indicator != null && indicator.getSourceProviderCode() != null
							? indicator.getSourceProviderCode()
							: indicatorCode;
					IndicatorProvider provider = providerRegistry.get(providerCode);
					Map<String, Object> parameters = parameterResolver.resolve(indicatorCode, condition,
							evaluationContext);
					parameters.putIfAbsent("indicatorCode", indicatorCode);
					parameters.putIfAbsent("sourceProviderCode", providerCode);
					parameters.put("tickers", List.copyOf(tickers));
					if (indicator != null) {
						if (indicator.getFieldExpression() != null)
							parameters.putIfAbsent("fieldExpression", indicator.getFieldExpression());
						if (indicator.getRowFilterExpression() != null)
							parameters.putIfAbsent("rowFilterExpression", indicator.getRowFilterExpression());
						if (indicator.getRowAggregation() != null)
							parameters.putIfAbsent("rowAggregation", indicator.getRowAggregation());
					}
					Object value = provider.getValue(evaluationContext, parameters);
					if (!(value instanceof Map<?, ?> providerValues)) {
						log.warn("MARKET provider {} returned non-map value for {}",
								provider.getClass().getSimpleName(), indicatorCode);
						continue;
					}
					for (Map.Entry<?, ?> entry : providerValues.entrySet()) {
						if (entry.getKey() != null && valuesByTicker.containsKey(String.valueOf(entry.getKey()))) {
							valuesByTicker.get(String.valueOf(entry.getKey())).put(indicatorCode, entry.getValue());
						}
					}
				} catch (Exception e) {
					log.warn("Unable to resolve MARKET indicator {}. Reason: {}", indicatorCode, e.getMessage(), e);
				}
			}
		}
		return valuesByTicker;
	}

	private IndicatorEvaluationContext createEvaluationContext(String subjectType, SubjectContext context) {
		return new IndicatorEvaluationContext(subjectType, context.getSubjectId(), context.getAsOfDate());
	}

	private boolean matched(SubjectResult result) {
		return result.outputs().values().stream().anyMatch(value -> Boolean.TRUE.equals(value));
	}

	private Object parse(String value, String type) {
		if (value == null)
			return null;
		return switch (type) {
		case "NUMBER" -> new BigDecimal(value);
		case "BOOLEAN" -> Boolean.valueOf(value);
		default -> value;
		};
	}
}
