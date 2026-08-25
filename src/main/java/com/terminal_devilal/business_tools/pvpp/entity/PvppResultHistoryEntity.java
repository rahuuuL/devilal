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

    @Column(name = "return_pct")
    private Double returnPct;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "clv")
    private Double clv;

    @Column(name = "centered_clv")
    private Double centeredClv;

    @Column(name = "rvol")
    private Double rvol;

    @Column(name = "efficiency")
    private Double efficiency;

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

    public Double getReturnPct() {
        return returnPct;
    }

    public void setReturnPct(Double returnPct) {
        this.returnPct = returnPct;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Double getClv() {
        return clv;
    }

    public void setClv(Double clv) {
        this.clv = clv;
    }

    public Double getCenteredClv() {
        return centeredClv;
    }

    public void setCenteredClv(Double centeredClv) {
        this.centeredClv = centeredClv;
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
}
