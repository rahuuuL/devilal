package com.terminal_devilal.business_tools.ratio_analysis.dto;

import com.terminal_devilal.business_tools.ratio_analysis.entity.SharpeSortinoGenerationStatus;

public class SharpeSortinoGenerationHistoryStatusUpdateRequest {

    private SharpeSortinoGenerationStatus status;

    public SharpeSortinoGenerationStatus getStatus() {
        return status;
    }

    public void setStatus(SharpeSortinoGenerationStatus status) {
        this.status = status;
    }
}
