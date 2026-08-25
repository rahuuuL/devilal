package com.terminal_devilal.business_tools.pvpp.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(PvppDailyId.class)
@Table(name = "pvpp_daily")
public class PvppDailyEntity {

    @Id
    @Column(name = "ticker")
    private String ticker;

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "return_pct")
    private Double returnPct;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "clv")
    private Double clv;

    @Column(name = "centered_clv")
    private Double centeredClv;

    @Column(name = "return_z")
    private Double returnZ;

    @Column(name = "clv_z")
    private Double clvZ;

    public PvppDailyEntity() {
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

    public Double getReturnZ() {
        return returnZ;
    }

    public void setReturnZ(Double returnZ) {
        this.returnZ = returnZ;
    }

    public Double getClvZ() {
        return clvZ;
    }

    public void setClvZ(Double clvZ) {
        this.clvZ = clvZ;
    }
}
