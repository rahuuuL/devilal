package com.terminal_devilal.business_tools.ratio_analysis.dto;

public class SharpeSortinoConfigRequest {

    private Integer days;
    private Double riskFreeRatePercent;
    private Boolean enabled;
    private String description;

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Double getRiskFreeRatePercent() {
        return riskFreeRatePercent;
    }

    public void setRiskFreeRatePercent(Double riskFreeRatePercent) {
        this.riskFreeRatePercent = riskFreeRatePercent;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
