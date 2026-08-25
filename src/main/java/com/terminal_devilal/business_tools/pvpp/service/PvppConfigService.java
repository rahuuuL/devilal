package com.terminal_devilal.business_tools.pvpp.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.business_tools.pvpp.dto.PvppConfigRequest;
import com.terminal_devilal.business_tools.pvpp.dto.PvppConfigResponse;
import com.terminal_devilal.business_tools.pvpp.entity.PvppConfigEntity;
import com.terminal_devilal.business_tools.pvpp.repository.PvppConfigRepository;

@Service
public class PvppConfigService {

    private final PvppConfigRepository pvppConfigRepository;

    public PvppConfigService(PvppConfigRepository pvppConfigRepository) {
        this.pvppConfigRepository = pvppConfigRepository;
    }

    @Transactional
    public PvppConfigResponse create(PvppConfigRequest request) {
        PvppConfigEntity entity = new PvppConfigEntity();
        entity.setDays(request.getDays());
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : Boolean.TRUE);
        entity.setDescription(request.getDescription());
        return toResponse(pvppConfigRepository.save(entity));
    }

    @Transactional
    public PvppConfigResponse update(Integer days, PvppConfigRequest request) {
        PvppConfigEntity entity = pvppConfigRepository.findById(days)
                .orElseThrow(() -> new IllegalArgumentException("Config not found for days: " + days));
        entity.setDays(request.getDays());
        entity.setEnabled(request.getEnabled() != null ? request.getEnabled() : entity.getEnabled());
        entity.setDescription(request.getDescription());
        return toResponse(pvppConfigRepository.save(entity));
    }

    public List<PvppConfigResponse> list() {
        return pvppConfigRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PvppConfigResponse get(Integer days) {
        return pvppConfigRepository.findById(days)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Config not found for days: " + days));
    }

    @Transactional
    public PvppConfigResponse setEnabled(Integer days, Boolean enabled) {
        PvppConfigEntity entity = pvppConfigRepository.findById(days)
                .orElseThrow(() -> new IllegalArgumentException("Config not found for days: " + days));
        entity.setEnabled(enabled != null ? enabled : Boolean.TRUE);
        return toResponse(pvppConfigRepository.save(entity));
    }

    @Transactional
    public void delete(Integer days) {
        pvppConfigRepository.deleteById(days);
    }

    private PvppConfigResponse toResponse(PvppConfigEntity entity) {
        return new PvppConfigResponse(entity.getDays(), entity.getEnabled(), entity.getDescription());
    }
}
