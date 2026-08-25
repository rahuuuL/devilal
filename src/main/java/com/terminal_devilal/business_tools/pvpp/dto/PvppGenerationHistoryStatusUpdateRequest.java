package com.terminal_devilal.business_tools.pvpp.dto;

import com.terminal_devilal.business_tools.pvpp.entity.PvppGenerationStatus;

public class PvppGenerationHistoryStatusUpdateRequest {

    private PvppGenerationStatus status;

    public PvppGenerationStatus getStatus() {
        return status;
    }

    public void setStatus(PvppGenerationStatus status) {
        this.status = status;
    }
}
