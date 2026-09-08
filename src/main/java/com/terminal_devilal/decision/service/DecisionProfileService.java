package com.terminal_devilal.decision.service;

import com.terminal_devilal.decision.api.DecisionRequests.*;
import com.terminal_devilal.decision.api.DecisionResponses.*;
import com.terminal_devilal.decision.entity.*;
import com.terminal_devilal.decision.exception.DecisionException;
import com.terminal_devilal.decision.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DecisionProfileService {
    private final DecisionProfileRepository profiles;
    private final DecisionOutputVariableRepository outputs;
    public DecisionProfileService(DecisionProfileRepository profiles,DecisionOutputVariableRepository outputs){this.profiles=profiles;this.outputs=outputs;}
    public List<ProfileResponse> list(Long ownerId){return profiles.findByOwnerIdOrderByCode(ownerId).stream().map(this::response).toList();}
    @Transactional public ProfileResponse create(ProfileRequest r){if(profiles.findByOwnerIdAndCode(r.ownerId(),r.code()).isPresent())throw new DecisionException("PROFILE_EXISTS","Profile code already exists: "+r.code());return response(profiles.save(new DecisionProfileEntity(r.ownerId(),r.code(),r.name(),r.description(),r.evaluationMode(),r.subjectType())));}
    @Transactional public ProfileResponse update(UUID id,ProfileRequest r){DecisionProfileEntity p=find(id);p.update(r.name(),r.description(),r.evaluationMode(),r.subjectType(),r.enabled());return response(p);}
    public DecisionProfileEntity find(UUID id){return profiles.findByPublicId(id).orElseThrow(()->new DecisionException("PROFILE_NOT_FOUND","Profile not found: "+id));}
    public DecisionProfileEntity find(Long ownerId,String code){return profiles.findByOwnerIdAndCode(ownerId,code).orElseThrow(()->new DecisionException("PROFILE_NOT_FOUND","Profile not found: "+code));}
    @Transactional public OutputResponse addOutput(OutputRequest r){DecisionProfileEntity p=find(r.ownerId(),r.profileCode());if(outputs.findByProfileIdAndCode(p.getId(),r.code()).isPresent())throw new DecisionException("OUTPUT_EXISTS","Output already exists: "+r.code());return outputResponse(outputs.save(new DecisionOutputVariableEntity(p,r.code(),r.name(),r.valueType(),r.initialValue(),r.minValue(),r.maxValue())));}
    public List<OutputResponse> outputs(Long ownerId,String code){DecisionProfileEntity p=find(ownerId,code);return outputs.findByProfileIdOrderByCode(p.getId()).stream().map(this::outputResponse).toList();}
    private ProfileResponse response(DecisionProfileEntity p){return new ProfileResponse(p.getPublicId(),p.getOwnerId(),p.getCode(),p.getName(),p.getDescription(),p.getEvaluationMode(),p.getSubjectType(),p.getEnabled());}
    private OutputResponse outputResponse(DecisionOutputVariableEntity o){return new OutputResponse(o.getId(),o.getProfile().getCode(),o.getCode(),o.getName(),o.getValueType(),o.getInitialValue(),o.getMinValue(),o.getMaxValue());}
}
