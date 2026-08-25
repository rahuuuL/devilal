package com.terminal_devilal.business_tools.pvpp.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(PvppResultHistoryId.class)
@Table(name = "pvpp_result_history")
public class PvppResultHistoryEntity {

    @Id
    @Column(name = "ticker")
    private String ticker;

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Id
    @Column(name = "days")
    private Integer days;

    @Column(name = "rvol")
    private Double rvol;

    @Column(name = "efficiency")
    private Double efficiency;

    @Column(name = "log_rvol_z")
    private Double logRvolZ;

    @Column(name = "pressure_score")
    private Double pressureScore;

    public PvppResultHistoryEntity() {
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Double getRvol() {
        return rvol;
    }

    public void setRvol(Double rvol) {
        this.rvol = rvol;
    }

    public Double getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(Double efficiency) {
        this.efficiency = efficiency;
    }

    public Double getLogRvolZ() {
        return logRvolZ;
    }

    public void setLogRvolZ(Double logRvolZ) {
        this.logRvolZ = logRvolZ;
    }

    public Double getPressureScore() {
        return pressureScore;
    }

    public void setPressureScore(Double pressureScore) {
        this.pressureScore = pressureScore;
    }
}
