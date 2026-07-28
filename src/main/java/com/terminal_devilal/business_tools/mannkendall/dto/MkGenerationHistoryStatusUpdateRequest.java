package com.terminal_devilal.business_tools.mannkendall.dto;

import com.terminal_devilal.business_tools.mannkendall.entity.MkGenerationStatus;

public class MkGenerationHistoryStatusUpdateRequest {

    private MkGenerationStatus status;

    public MkGenerationStatus getStatus() {
        return status;
    }

    public void setStatus(MkGenerationStatus status) {
        this.status = status;
    }
}