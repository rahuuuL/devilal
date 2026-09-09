package com.terminal_devilal.decision.service;

import com.terminal_devilal.decision.api.DecisionRequests.IndicatorPatchRequest;
import com.terminal_devilal.decision.api.DecisionRequests.IndicatorRequest;
import com.terminal_devilal.decision.api.DecisionResponses.IndicatorResponse;
import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.exception.DecisionException;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DecisionIndicatorService {
    private final DecisionIndicatorRepository repository;
    public DecisionIndicatorService(DecisionIndicatorRepository repository){this.repository=repository;}
    public List<IndicatorResponse> list(String subjectType){return (subjectType==null?repository.findByEnabledTrueOrderByCategoryAscNameAsc():repository.findBySubjectTypeAndEnabledTrueOrderByCategoryAscNameAsc(subjectType)).stream().map(this::response).toList();}
    @Transactional public IndicatorResponse save(IndicatorRequest request){
        DecisionIndicatorEntity entity=repository.findById(request.code()).orElseGet(()->new DecisionIndicatorEntity(request.code(),request.name(),request.category(),request.subjectType(),request.valueType(),request.unit(),request.sourceType(),request.sourceReference(),request.minValue(),request.maxValue(),request.description()));
        if(repository.existsById(request.code())) entity.update(request.name(),request.category(),request.subjectType(),request.valueType(),request.unit(),request.sourceType(),request.sourceReference(),request.sourceProviderCode(),request.rowFilterExpression(),request.fieldExpression(),request.rowAggregation(),request.minValue(),request.maxValue(),request.description(),request.enabled());
        else {
            entity.setSourceProviderCode(request.sourceProviderCode());
            entity.setRowFilterExpression(request.rowFilterExpression());
            entity.setFieldExpression(request.fieldExpression());
            entity.setRowAggregation(request.rowAggregation());
            if(request.enabled()!=null) entity.setEnabled(request.enabled());
        }
        return response(repository.save(entity));
    }
    @Transactional public IndicatorResponse setEnabled(String code, boolean enabled){DecisionIndicatorEntity e=repository.findById(code).orElseThrow(()->new DecisionException("INDICATOR_NOT_FOUND","Indicator not found: "+code));e.setEnabled(enabled);return response(e);}
    @Transactional public IndicatorResponse patch(String code, IndicatorPatchRequest request){
        DecisionIndicatorEntity entity=repository.findById(code).orElseThrow(()->new DecisionException("INDICATOR_NOT_FOUND","Indicator not found: "+code));
        entity.update(value(request.name(),entity.getName()),value(request.category(),entity.getCategory()),value(request.subjectType(),entity.getSubjectType()),value(request.valueType(),entity.getValueType()),request.unit()==null?entity.getUnit():request.unit(),value(request.sourceType(),entity.getSourceType()),request.sourceReference()==null?entity.getSourceReference():request.sourceReference(),request.sourceProviderCode()==null?entity.getSourceProviderCode():request.sourceProviderCode(),request.rowFilterExpression()==null?entity.getRowFilterExpression():request.rowFilterExpression(),request.fieldExpression()==null?entity.getFieldExpression():request.fieldExpression(),request.rowAggregation()==null?entity.getRowAggregation():request.rowAggregation(),request.minValue()==null?entity.getMinValue():request.minValue(),request.maxValue()==null?entity.getMaxValue():request.maxValue(),request.description()==null?entity.getDescription():request.description(),request.enabled()==null?entity.getEnabled():request.enabled());
        return response(entity);
    }
    private String value(String incoming,String current){return incoming==null?current:incoming;}
    private IndicatorResponse response(DecisionIndicatorEntity e){return new IndicatorResponse(e.getCode(),e.getName(),e.getCategory(),e.getSubjectType(),e.getValueType(),e.getUnit(),e.getSourceType(),e.getSourceReference(),e.getSourceProviderCode(),e.getRowFilterExpression(),e.getFieldExpression(),e.getRowAggregation(),e.getEnabled(),e.getMinValue(),e.getMaxValue(),e.getDescription(),e.getCreatedAt(),e.getUpdatedAt());}
}
