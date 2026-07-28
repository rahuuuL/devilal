package com.terminal_devilal.business_tools.mannkendall.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.business_tools.mannkendall.dto.MkConfigRequest;
import com.terminal_devilal.business_tools.mannkendall.dto.MkConfigResponse;
import com.terminal_devilal.business_tools.mannkendall.entity.MkConfigEntity;
import com.terminal_devilal.business_tools.mannkendall.repository.MkConfigRepository;

@Service
public class MkConfigService {

    private final MkConfigRepository mkConfigRepository;

    public MkConfigService(MkConfigRepository mkConfigRepository) {
        this.mkConfigRepository = mkConfigRepository;
    }

    @Transactional
    public MkConfigResponse create(MkConfigRequest request) {
        MkConfigEntity entity = new MkConfigEntity();
        entity.setDays(request.getDays());
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : Boolean.TRUE);
        entity.setDescription(request.getDescription());
        return toResponse(mkConfigRepository.save(entity));
    }

    @Transactional
    public MkConfigResponse update(Integer days, MkConfigRequest request) {
        MkConfigEntity entity = mkConfigRepository.findById(days)
                .orElseThrow(() -> new IllegalArgumentException("Config not found for days: " + days));
        entity.setDays(request.getDays());
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : entity.getEnabled());
        entity.setDescription(request.getDescription());
        return toResponse(mkConfigRepository.save(entity));
    }

    public List<MkConfigResponse> list() {
        return mkConfigRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public MkConfigResponse get(Integer days) {
        return mkConfigRepository.findById(days).map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Config not found for days: " + days));
    }

    @Transactional
    public MkConfigResponse setEnabled(Integer days, Boolean enabled) {
        MkConfigEntity entity = mkConfigRepository.findById(days)
                .orElseThrow(() -> new IllegalArgumentException("Config not found for days: " + days));
        entity.setEnabled(enabled != null ? enabled : Boolean.TRUE);
        return toResponse(mkConfigRepository.save(entity));
    }

    @Transactional
    public void delete(Integer days) {
        mkConfigRepository.deleteById(days);
    }

    private MkConfigResponse toResponse(MkConfigEntity entity) {
        return new MkConfigResponse(entity.getDays(), entity.getEnabled(), entity.getDescription());
    }
}
