package com.terminal_devilal.business_tools.ratio_analysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sharpe_sortino_config")
public class SharpeSortinoConfigEntity {

    @Id
    @Column(name = "days")
    private Integer days;

    @Column(name = "risk_free_rate_percent", nullable = false)
    private Double riskFreeRatePercent = 6.0;

    @Column(name = "enabled")
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "description")
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
