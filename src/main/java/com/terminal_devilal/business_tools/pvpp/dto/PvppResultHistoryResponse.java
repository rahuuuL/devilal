package com.terminal_devilal.business_tools.pvpp.dto;

import java.time.LocalDate;

public class PvppResultHistoryResponse {

    private String ticker;
    private LocalDate date;
    private Integer days;
    private Double returnPct;
    private Double volume;
    private Double clv;
    private Double centeredClv;
    private Double returnZ;
    private Double clvZ;
    private Double rvol;
    private Double efficiency;
    private Double logRvolZ;
    private Double pressureScore;

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

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
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
