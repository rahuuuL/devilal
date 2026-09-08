package com.terminal_devilal.decision.service;

import com.terminal_devilal.decision.api.DecisionRequests.*;
import com.terminal_devilal.decision.api.DecisionResponses.*;
import com.terminal_devilal.decision.entity.*;
import com.terminal_devilal.decision.indicator.IndicatorEvaluationContext;
import com.terminal_devilal.decision.indicator.IndicatorParameterResolver;
import com.terminal_devilal.decision.indicator.IndicatorProvider;
import com.terminal_devilal.decision.indicator.IndicatorProviderRegistry;
import com.terminal_devilal.decision.model.*;
import com.terminal_devilal.decision.repository.DecisionOutputVariableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class DecisionExecutionService {
    private static final Logger log = LoggerFactory.getLogger(DecisionExecutionService.class);

    private final DecisionRuleService ruleService;
    private final DecisionOutputVariableRepository outputRepository;
    private final DecisionRuleEvaluator evaluator;
    private final IndicatorProviderRegistry providerRegistry;
    private final IndicatorParameterResolver parameterResolver;

    public DecisionExecutionService(
            DecisionRuleService ruleService,
            DecisionOutputVariableRepository outputRepository,
            DecisionRuleEvaluator evaluator,
            IndicatorProviderRegistry providerRegistry,
            IndicatorParameterResolver parameterResolver) {
        this.ruleService = ruleService;
        this.outputRepository = outputRepository;
        this.evaluator = evaluator;
        this.providerRegistry = providerRegistry;
        this.parameterResolver = parameterResolver;
    }

    public EvaluationResponse evaluate(EvaluationRequest request){
        log.info("Starting evaluation: ownerId={}, profileCode={}, subjectCount={}, asOfDate={}",
                request.ownerId(), request.profileCode(), request.subjects() == null ? 0 : request.subjects().size(), request.asOfDate());
        DecisionProfileEntity profile=ruleService.profile(request.ownerId(),request.profileCode());
        if(!profile.getSubjectType().equals(request.subjectType()))throw new IllegalArgumentException("Subject type does not match profile");
        log.info("Loaded profile {} for subjectType={} with evaluationMode={}", profile.getCode(), profile.getSubjectType(), profile.getEvaluationMode());
        List<RuleDefinition> rules=ruleService.activeDefinitions(request.ownerId(),request.profileCode());
        log.info("Loaded {} active rules for profile {}", rules.size(), profile.getCode());
        List<SubjectResult> results=request.subjects().stream().map(subject->evaluateOne(profile,rules,subject,request.asOfDate())).toList();
        if("FILTER".equals(profile.getEvaluationMode())) results=results.stream().filter(this::matched).toList();
        log.info("Evaluation finished for profile {}. Final result count: {}", profile.getCode(), results.size());
        return new EvaluationResponse(profile.getCode(),request.asOfDate(),results);
    }

    private SubjectResult evaluateOne(DecisionProfileEntity profile,List<RuleDefinition> rules,SubjectRequest request,LocalDate requestDate){
        LocalDate date=request.asOfDate()==null?requestDate:request.asOfDate();
        log.info("Evaluating subject subjectType={}, subjectId={}, asOfDate={}", request.subjectType(), request.subjectId(), date);
        Map<String,Object> initial=new LinkedHashMap<>();
        for(DecisionOutputVariableEntity output:outputRepository.findByProfileIdOrderByCode(profile.getId()))
            initial.put(output.getCode(),parse(output.getInitialValue(),output.getValueType()));
        log.debug("Initial output values before rule evaluation: {}", initial);

        DecisionState state=new DecisionState(initial);
        SubjectContext baseContext = new SubjectContext(request.subjectType(),request.subjectId(),date,request.attributes());
        log.debug("Base subject context attributes: {}", baseContext.getAttributes());
        SubjectContext context = resolveProviderValues(rules, baseContext);
        log.info("Resolved context attributes before evaluation: {}", context.getAttributes());
        List<String> fired=evaluator.evaluateAndReturnFiredRules(rules,context,state);
        log.info("Rules fired for subject {} {}: {}", request.subjectType(), request.subjectId(), fired);
        log.info("Final outputs for subject {} {}: {}", request.subjectType(), request.subjectId(), state.getOutputs());
        return new SubjectResult(
            request.subjectType(),
            request.subjectId(),
            date,
            baseContext.getAttributes(),
            context.getAttributes(),
            state.getOutputs(),
            fired);
    }

    private SubjectContext resolveProviderValues(List<RuleDefinition> rules, SubjectContext context) {
        Map<String,Object> merged = new LinkedHashMap<>(context.getAttributes());
        log.debug("Starting provider-based resolution for {} {} with {} rule conditions", context.getSubjectType(), context.getSubjectId(), rules.size());
        for (RuleDefinition rule : rules) {
            for (RuleDefinition.Condition condition : rule.conditions()) {
                String indicatorCode = condition.indicator();
                if (merged.containsKey(indicatorCode)) {
                    log.debug("Indicator {} already present in request attributes; skipping provider resolution. Value={}", indicatorCode, merged.get(indicatorCode));
                    continue;
                }
                try {
                    IndicatorProvider provider = providerRegistry.get(indicatorCode);
                    log.debug("Resolving indicator {} via provider {}", indicatorCode, provider.getClass().getSimpleName());
                    IndicatorEvaluationContext evaluationContext = new IndicatorEvaluationContext(context.getSubjectType(), context.getSubjectId(), context.getAsOfDate());
                    Map<String, Object> parameters = parameterResolver.resolve(indicatorCode, condition, evaluationContext);
                    log.debug("Resolved provider parameters for {}: {}", indicatorCode, parameters);
                    Object value = provider.getValue(evaluationContext, parameters);
                    if (value != null) {
                        merged.put(indicatorCode, value);
                        log.info("Indicator {} resolved from provider {} with value {}", indicatorCode, provider.getClass().getSimpleName(), value);
                    } else {
                        log.warn("Indicator {} resolved to null from provider {}", indicatorCode, provider.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    log.warn("Unable to resolve indicator {} from provider during rule execution. Reason: {}", indicatorCode, e.getMessage(), e);
                    // Ignore missing provider or unsupported indicator; evaluator will later decide if condition can be evaluated.
                }
            }
        }
        return new SubjectContext(context.getSubjectType(), context.getSubjectId(), context.getAsOfDate(), merged);
    }

    private boolean matched(SubjectResult result){return result.outputs().values().stream().anyMatch(value->Boolean.TRUE.equals(value));}
    private Object parse(String value,String type){if(value==null)return null;return switch(type){case "NUMBER"->new BigDecimal(value);case "BOOLEAN"->Boolean.valueOf(value);default->value;};}
}
