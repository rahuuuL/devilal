package com.terminal_devilal.business_tools.ratio_analysis.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoConfigRequest;
import com.terminal_devilal.business_tools.ratio_analysis.dto.SharpeSortinoConfigResponse;
import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoConfigEntity;
import com.terminal_devilal.business_tools.ratio_analysis.repository.SharpeSortinoConfigRepository;

@Service
public class SharpeSortinoConfigService {

    private final SharpeSortinoConfigRepository repository;

    public SharpeSortinoConfigService(SharpeSortinoConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SharpeSortinoConfigResponse create(SharpeSortinoConfigRequest request) {
        validateDays(request.getDays());
        validateRiskFreeRate(request.getRiskFreeRatePercent());
        SharpeSortinoConfigEntity entity = new SharpeSortinoConfigEntity();
        entity.setDays(request.getDays());
        entity.setRiskFreeRatePercent(
                request.getRiskFreeRatePercent() == null ? 6.0 : request.getRiskFreeRatePercent());
        entity.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
        entity.setDescription(request.getDescription());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public SharpeSortinoConfigResponse update(Integer days, SharpeSortinoConfigRequest request) {
        SharpeSortinoConfigEntity entity = repository.findById(days)
                .orElseThrow(() -> new IllegalArgumentException("Config not found for days: " + days));
        validateDays(request.getDays());
        validateRiskFreeRate(request.getRiskFreeRatePercent());
        entity.setDays(request.getDays());
        if (request.getRiskFreeRatePercent() != null) {
            entity.setRiskFreeRatePercent(request.getRiskFreeRatePercent());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        entity.setDescription(request.getDescription());
        return toResponse(repository.save(entity));
    }

    public List<SharpeSortinoConfigResponse> list() {
        return repository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public SharpeSortinoConfigResponse get(Integer days) {
        return repository.findById(days).map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Config not found for days: " + days));
    }

    @Transactional
    public SharpeSortinoConfigResponse setEnabled(Integer days, Boolean enabled) {
        SharpeSortinoConfigEntity entity = repository.findById(days)
                .orElseThrow(() -> new IllegalArgumentException("Config not found for days: " + days));
        entity.setEnabled(enabled == null ? Boolean.TRUE : enabled);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Integer days) {
        repository.deleteById(days);
    }

    private void validateDays(Integer days) {
        if (days == null || days <= 0) {
            throw new IllegalArgumentException("days must be positive");
        }
    }

    private void validateRiskFreeRate(Double riskFreeRatePercent) {
        if (riskFreeRatePercent != null && !Double.isFinite(riskFreeRatePercent)) {
            throw new IllegalArgumentException("riskFreeRatePercent must be finite");
        }
    }

    private SharpeSortinoConfigResponse toResponse(SharpeSortinoConfigEntity entity) {
        return new SharpeSortinoConfigResponse(entity.getDays(), entity.getRiskFreeRatePercent(),
                entity.getEnabled(), entity.getDescription());
    }
}
