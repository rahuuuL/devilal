package com.terminal_devilal.business_tools.pvpp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pvpp_config")
public class PvppConfigEntity {

    @Id
    @Column(name = "days")
    private Integer days;

    @Column(name = "enabled")
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "description")
    private String description;

    public PvppConfigEntity() {
    }

    public PvppConfigEntity(Integer days, Boolean enabled, String description) {
        this.days = days;
        this.enabled = enabled != null ? enabled : Boolean.TRUE;
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
