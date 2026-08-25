package com.terminal_devilal.business_tools.pvpp.dto;

public class PvppConfigResponse {

    private Integer days;
    private Boolean enabled;
    private String description;

    public PvppConfigResponse() {
    }

    public PvppConfigResponse(Integer days, Boolean enabled, String description) {
        this.days = days;
        this.enabled = enabled;
        this.description = description;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
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
