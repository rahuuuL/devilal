package com.terminal_devilal.business_tools.ratio_analysis.dto;

public class SharpeSortinoConfigResponse {

    private final Integer days;
    private final Double riskFreeRatePercent;
    private final Boolean enabled;
    private final String description;

    public SharpeSortinoConfigResponse(Integer days, Double riskFreeRatePercent, Boolean enabled, String description) {
        this.days = days;
        this.riskFreeRatePercent = riskFreeRatePercent;
        this.enabled = enabled;
        this.description = description;
    }

    public Integer getDays() {
        return days;
    }

    public Double getRiskFreeRatePercent() {
        return riskFreeRatePercent;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public String getDescription() {
        return description;
    }
}
