package com.terminal_devilal.decision.service;

import com.terminal_devilal.decision.api.DecisionRequests.IndicatorParameterRequest;
import com.terminal_devilal.decision.api.DecisionRequests.IndicatorPatchRequest;
import com.terminal_devilal.decision.api.DecisionRequests.IndicatorRequest;
import com.terminal_devilal.decision.api.DecisionResponses.IndicatorParameterResponse;
import com.terminal_devilal.decision.api.DecisionResponses.IndicatorResponse;
import com.terminal_devilal.decision.entity.DecisionIndicatorEntity;
import com.terminal_devilal.decision.entity.DecisionIndicatorParameterEntity;
import com.terminal_devilal.decision.exception.DecisionException;
import com.terminal_devilal.decision.repository.DecisionIndicatorParameterRepository;
import com.terminal_devilal.decision.repository.DecisionIndicatorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DecisionIndicatorService {
    private final DecisionIndicatorRepository repository;
    private final DecisionIndicatorParameterRepository parameterRepository;
    public DecisionIndicatorService(DecisionIndicatorRepository repository, DecisionIndicatorParameterRepository parameterRepository){this.repository=repository;this.parameterRepository=parameterRepository;}
    public List<IndicatorResponse> list(String subjectType){return (subjectType==null?repository.findByEnabledTrueOrderByCategoryAscNameAsc():repository.findBySubjectTypeAndEnabledTrueOrderByCategoryAscNameAsc(subjectType)).stream().map(this::response).toList();}
    public IndicatorResponse get(String code){DecisionIndicatorEntity entity=repository.findById(code).orElseThrow(()->new DecisionException("INDICATOR_NOT_FOUND","Indicator not found: "+code));return response(entity);}
    public List<IndicatorParameterResponse> parameters(String indicatorCode){
        return parameterRepository.findByIndicatorCodeOrderBySequenceNoAsc(indicatorCode).stream().map(this::parameterResponse).toList();
    }
    @Transactional public IndicatorParameterResponse createParameter(String indicatorCode, IndicatorParameterRequest request){
        if(request==null) throw new IllegalArgumentException("Request is required");
        if(request.indicatorCode()!=null && !indicatorCode.equals(request.indicatorCode())) throw new IllegalArgumentException("Path indicatorCode and body indicatorCode differ");
        if(request.parameterCode()==null || request.parameterCode().isBlank()) throw new IllegalArgumentException("parameterCode is required");
        DecisionIndicatorParameterEntity entity = parameterRepository.findByIndicatorCodeAndParameterCode(indicatorCode, request.parameterCode())
                .orElseGet(() -> new DecisionIndicatorParameterEntity(indicatorCode, request.parameterCode(), request.parameterName(), request.valueType(), request.required(), request.defaultValueJson(), request.resolutionType(), request.dynamicExpression(), request.minValue(), request.maxValue(), request.description(), request.sequenceNo(), request.enabled()));
        entity.update(indicatorCode, request.parameterCode(), request.parameterName(), request.valueType(), request.required(), request.defaultValueJson(), request.resolutionType(), request.dynamicExpression(), request.minValue(), request.maxValue(), request.description(), request.sequenceNo(), request.enabled());
        return parameterResponse(parameterRepository.save(entity));
    }
    @Transactional public IndicatorParameterResponse upsertParameter(String indicatorCode, String parameterCode, IndicatorParameterRequest request){
        if(request==null) throw new IllegalArgumentException("Request is required");
        if(request.indicatorCode()!=null && !indicatorCode.equals(request.indicatorCode())) throw new IllegalArgumentException("Path indicatorCode and body indicatorCode differ");
        if(parameterCode==null || parameterCode.isBlank()) throw new IllegalArgumentException("parameterCode is required");
        DecisionIndicatorParameterEntity entity = parameterRepository.findByIndicatorCodeAndParameterCode(indicatorCode, parameterCode)
                .orElseGet(() -> new DecisionIndicatorParameterEntity(indicatorCode, parameterCode, request.parameterName(), request.valueType(), request.required(), request.defaultValueJson(), request.resolutionType(), request.dynamicExpression(), request.minValue(), request.maxValue(), request.description(), request.sequenceNo(), request.enabled()));
        entity.update(indicatorCode, parameterCode, request.parameterName(), request.valueType(), request.required(), request.defaultValueJson(), request.resolutionType(), request.dynamicExpression(), request.minValue(), request.maxValue(), request.description(), request.sequenceNo(), request.enabled());
        return parameterResponse(parameterRepository.save(entity));
    }
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
    private IndicatorParameterResponse parameterResponse(DecisionIndicatorParameterEntity e){return new IndicatorParameterResponse(e.getId(),e.getIndicatorCode(),e.getParameterCode(),e.getParameterName(),e.getValueType(),e.getRequired(),e.getDefaultValueJson(),e.getResolutionType(),e.getDynamicExpression(),e.getMinValue(),e.getMaxValue(),e.getDescription(),e.getSequenceNo(),e.getEnabled(),e.getCreatedAt(),e.getUpdatedAt());}
}
