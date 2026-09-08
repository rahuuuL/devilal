package com.terminal_devilal.decision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terminal_devilal.decision.api.DecisionRequests.RuleRequest;
import com.terminal_devilal.decision.api.DecisionResponses.*;
import com.terminal_devilal.decision.entity.*;
import com.terminal_devilal.decision.exception.DecisionException;
import com.terminal_devilal.decision.model.RuleDefinition;
import com.terminal_devilal.decision.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DecisionRuleService {
    private final DecisionRuleRepository rules;
    private final DecisionRuleVersionRepository versions;
    private final DecisionIndicatorRepository indicators;
    private final DecisionOutputVariableRepository outputs;
    private final DecisionProfileService profileService;
    private final ObjectMapper mapper;
    public DecisionRuleService(DecisionRuleRepository rules,DecisionRuleVersionRepository versions,DecisionIndicatorRepository indicators,DecisionOutputVariableRepository outputs,DecisionProfileService profileService,ObjectMapper mapper){this.rules=rules;this.versions=versions;this.indicators=indicators;this.outputs=outputs;this.profileService=profileService;this.mapper=mapper;}
    public List<RuleResponse> list(Long ownerId){return rules.findByOwnerIdOrderByPriorityDescCodeAsc(ownerId).stream().map(this::response).toList();}
    public RuleResponse get(UUID id){return response(find(id));}
    @Transactional public RuleResponse create(RuleRequest r){
        if(rules.findByOwnerIdAndCode(r.ownerId(),r.code()).isPresent())throw new DecisionException("RULE_EXISTS","Rule code already exists: "+r.code());
        DecisionProfileEntity profile=profileService.find(r.ownerId(),r.profileCode());
        validateDefinition(profile,r.definition());
        String json=write(r.definition()); DecisionRuleEntity rule=rules.save(new DecisionRuleEntity(r.ownerId(),profile,r.code(),r.name(),r.ruleType(),r.priority(),json));
        versions.save(new DecisionRuleVersionEntity(rule,1,json,"DRAFT")); return response(rule);
    }
    @Transactional public RuleResponse update(UUID id,RuleRequest r){DecisionRuleEntity rule=find(id);if(!rule.getOwnerId().equals(r.ownerId()))throw new DecisionException("FORBIDDEN","Rule does not belong to owner");validateDefinition(rule.getProfile(),r.definition());rule.update(r.name(),r.ruleType(),r.priority(),write(r.definition()));versions.save(new DecisionRuleVersionEntity(rule,rule.getCurrentVersion(),rule.getDefinitionJson(),"DRAFT"));return response(rule);}
    @Transactional public RuleResponse validate(UUID id){DecisionRuleEntity rule=find(id);JsonNode node=read(rule.getDefinitionJson());validateDefinition(rule.getProfile(),node);rule.setStatus("VALIDATED");currentVersion(rule).setStatus("VALIDATED");return response(rule);}
    @Transactional public RuleResponse activate(UUID id){DecisionRuleEntity rule=find(id);if(!"VALIDATED".equals(rule.getStatus()))throw new DecisionException("RULE_NOT_VALIDATED","Rule must be validated before activation");rule.setStatus("ACTIVE");rule.setEnabled(true);currentVersion(rule).setStatus("ACTIVE");return response(rule);}
    @Transactional public RuleResponse disable(UUID id){DecisionRuleEntity rule=find(id);rule.setStatus("DISABLED");rule.setEnabled(false);return response(rule);}
    public List<VersionResponse> versions(UUID id){DecisionRuleEntity rule=find(id);return versions.findByRuleIdOrderByVersionDesc(rule.getId()).stream().map(v->new VersionResponse(v.getVersion(),v.getStatus(),v.getDefinitionJson())).toList();}
    public VersionResponse currentVersionResponse(UUID id){DecisionRuleEntity rule=find(id);return versions.findByRuleIdAndVersion(rule.getId(),rule.getCurrentVersion()).map(version->new VersionResponse(version.getVersion(),version.getStatus(),version.getDefinitionJson())).orElseGet(()->new VersionResponse(rule.getCurrentVersion(),rule.getStatus(),rule.getDefinitionJson()));}
    public List<RuleDefinition> activeDefinitions(Long ownerId,String profileCode){DecisionProfileEntity p=profileService.find(ownerId,profileCode);return rules.findByProfileIdAndStatusAndEnabledTrueOrderByPriorityDesc(p.getId(),"ACTIVE").stream().map(r->{RuleDefinition definition=readDefinition(r.getDefinitionJson());return new RuleDefinition(r.getName(),r.getPriority(),definition.conditions(),definition.actions());}).toList();}
    public DecisionProfileEntity profile(Long ownerId,String code){return profileService.find(ownerId,code);}
    private DecisionRuleEntity find(UUID id){return rules.findByPublicId(id).orElseThrow(()->new DecisionException("RULE_NOT_FOUND","Rule not found: "+id));}
    private DecisionRuleVersionEntity currentVersion(DecisionRuleEntity rule){return versions.findByRuleIdAndVersion(rule.getId(),rule.getCurrentVersion()).orElseThrow(()->new DecisionException("RULE_VERSION_NOT_FOUND","Current rule version not found: "+rule.getCurrentVersion()));}
    private RuleResponse response(DecisionRuleEntity r){return new RuleResponse(r.getPublicId(),r.getOwnerId(),r.getProfile().getCode(),r.getCode(),r.getName(),r.getRuleType(),r.getPriority(),r.getStatus(),r.getEnabled(),r.getCurrentVersion());}
    private String write(JsonNode n){try{return mapper.writeValueAsString(n);}catch(Exception e){throw new DecisionException("INVALID_JSON","Rule definition cannot be serialized");}}
    private JsonNode read(String s){try{return mapper.readTree(s);}catch(Exception e){throw new DecisionException("INVALID_JSON","Stored rule definition is invalid");}}
    private RuleDefinition readDefinition(String s){try{return mapper.treeToValue(normalizeOperators(mapper.readTree(s)),RuleDefinition.class);}catch(Exception e){throw new DecisionException("INVALID_RULE","Rule definition does not match the supported model: "+e.getMessage());}}
    private JsonNode normalizeOperators(JsonNode input){
        JsonNode copy=input.deepCopy();
        if(copy.has("conditions")&&copy.get("conditions").isArray()) for(JsonNode condition:copy.get("conditions")){
            JsonNode operator=condition.get("operator");
            if(operator!=null&&operator.isTextual()) ((com.fasterxml.jackson.databind.node.ObjectNode)condition).put("operator",switch(operator.asText()){case ">"->"GREATER_THAN";case ">="->"GREATER_THAN_OR_EQUAL";case "<"->"LESS_THAN";case "<="->"LESS_THAN_OR_EQUAL";case "="->"EQUAL";default->operator.asText();});
        }
        return copy;
    }
    private void validateDefinition(DecisionProfileEntity profile,JsonNode n){
        if(n==null||!n.isObject())throw new DecisionException("INVALID_RULE","Definition must be an object");
        JsonNode conditions=n.get("conditions"), actions=n.get("actions"); if(conditions==null||!conditions.isArray()||actions==null||!actions.isArray()||actions.isEmpty())throw new DecisionException("INVALID_RULE","Definition requires conditions and at least one action");
        for(JsonNode c:conditions){String code=text(c,"indicator");validateIndicator(code);if(c.has("comparisonIndicator"))validateIndicator(c.get("comparisonIndicator").asText());if(!c.has("operator")||!c.has("value"))throw new DecisionException("INVALID_CONDITION","Condition requires operator and value");}
        for(JsonNode a:actions){String target=text(a,"target");var output=outputs.findByProfileIdAndCode(profile.getId(),target).orElseThrow(()->new DecisionException("OUTPUT_NOT_DECLARED","Output is not declared for profile: "+target));String type=text(a,"type");if(!List.of("SET","INCREASE","DECREASE","TAG","SELECT","EXCLUDE").contains(type))throw new DecisionException("UNSUPPORTED_ACTION","Unsupported action: "+type);if((type.equals("INCREASE")||type.equals("DECREASE"))&&!"NUMBER".equals(output.getValueType()))throw new DecisionException("OUTPUT_TYPE_MISMATCH","Numeric action requires a NUMBER output: "+target);if((type.equals("INCREASE")||type.equals("DECREASE"))&&!a.has("constantValue")&&!a.has("expression"))throw new DecisionException("INVALID_ACTION","Numeric action requires constantValue or expression");if(a.has("expression"))validateExpressionIndicators(a.get("expression"));}
        try{readDefinition(write(n));}catch(DecisionException e){throw e;}catch(Exception e){throw new DecisionException("INVALID_RULE","Definition cannot be parsed: "+e.getMessage());}
    }
    private String text(JsonNode n,String field){if(!n.hasNonNull(field)||!n.get(field).isTextual()||n.get(field).asText().isBlank())throw new DecisionException("INVALID_RULE","Missing field: "+field);return n.get(field).asText();}
    private void validateIndicator(String code){var indicator=indicators.findById(code).orElseThrow(()->new DecisionException("INDICATOR_NOT_FOUND","Indicator not registered: "+code));if(!indicator.getEnabled())throw new DecisionException("INDICATOR_DISABLED","Indicator is disabled: "+code);}
    private void validateExpressionIndicators(JsonNode node){if(node==null)return;if(node.has("indicator"))validateIndicator(node.get("indicator").asText());node.fields().forEachRemaining(entry->{if(entry.getValue().isObject()||entry.getValue().isArray())validateExpressionIndicators(entry.getValue());});}
}
