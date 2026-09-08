package com.terminal_devilal.decision.service;

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
        if(entity != null && repository.existsById(request.code())) entity.update(request.name(),request.category(),request.subjectType(),request.valueType(),request.unit(),request.sourceType(),request.sourceReference(),request.minValue(),request.maxValue(),request.description(),request.enabled());
        return response(repository.save(entity));
    }
    @Transactional public IndicatorResponse setEnabled(String code, boolean enabled){DecisionIndicatorEntity e=repository.findById(code).orElseThrow(()->new DecisionException("INDICATOR_NOT_FOUND","Indicator not found: "+code));e.setEnabled(enabled);return response(e);}
    private IndicatorResponse response(DecisionIndicatorEntity e){return new IndicatorResponse(e.getCode(),e.getName(),e.getCategory(),e.getSubjectType(),e.getValueType(),e.getUnit(),e.getSourceType(),e.getSourceReference(),e.getEnabled(),e.getMinValue(),e.getMaxValue(),e.getDescription());}
}
