package com.terminal_devilal.business_tools.mannkendall.calculator;

public class MannKendallCalcResult {

    private String trend;
    private boolean h;
    private double p;
    private double z;
    private double tau;
    private long s;
    private double varS;
    private double slope;
    private double intercept;

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public boolean isH() {
        return h;
    }

    public void setH(boolean h) {
        this.h = h;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getTau() {
        return tau;
    }

    public void setTau(double tau) {
        this.tau = tau;
    }

    public long getS() {
        return s;
    }

    public void setS(long s) {
        this.s = s;
    }

    public double getVarS() {
        return varS;
    }

    public void setVarS(double varS) {
        this.varS = varS;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    public double getIntercept() {
        return intercept;
    }

    public void setIntercept(double intercept) {
        this.intercept = intercept;
    }

    @Override
    public String toString() {
        return "MannKendallCalcResult [trend=" + trend + ", h=" + h + ", p=" + p + ", z=" + z + ", tau="
                + tau + ", s=" + s + ", varS=" + varS + ", slope=" + slope + ", intercept=" + intercept + "]";
    }
}