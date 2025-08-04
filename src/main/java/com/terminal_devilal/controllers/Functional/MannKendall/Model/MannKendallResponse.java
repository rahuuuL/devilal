package com.terminal_devilal.controllers.Functional.MannKendall.Model;

public class MannKendallResponse {

	private String trend;
	private Boolean h;
	private Double p;
	private Double z;
	private Double Tau;
	private Double s;
	private Double var_s;
	private Double slope;
	private Double intercept;

	public MannKendallResponse() {
	}

	public MannKendallResponse(String trend, Boolean h, Double p, Double z, Double tau, Double s, Double var_s,
			Double slope, Double intercept) {
		super();
		this.trend = trend;
		this.h = h;
		this.p = p;
		this.z = z;
		Tau = tau;
		this.s = s;
		this.var_s = var_s;
		this.slope = slope;
		this.intercept = intercept;
	}

	public String getTrend() {
		return trend;
	}

	public void setTrend(String trend) {
		this.trend = trend;
	}

	public Boolean getH() {
		return h;
	}

	public void setH(Boolean h) {
		this.h = h;
	}

	public Double getP() {
		return p;
	}

	public void setP(Double p) {
		this.p = p;
	}

	public Double getZ() {
		return z;
	}

	public void setZ(Double z) {
		this.z = z;
	}

	public Double getTau() {
		return Tau;
	}

	public void setTau(Double tau) {
		Tau = tau;
	}

	public Double getS() {
		return s;
	}

	public void setS(Double s) {
		this.s = s;
	}

	public Double getVar_s() {
		return var_s;
	}

	public void setVar_s(Double var_s) {
		this.var_s = var_s;
	}

	public Double getSlope() {
		return slope;
	}

	public void setSlope(Double slope) {
		this.slope = slope;
	}

	public Double getIntercept() {
		return intercept;
	}

	public void setIntercept(Double intercept) {
		this.intercept = intercept;
	}

	@Override
	public String toString() {
		return "MannKendallResponse [trend=" + trend + ", h=" + h + ", p=" + p + ", z=" + z + ", Tau=" + Tau + ", s="
				+ s + ", var_s=" + var_s + ", slope=" + slope + ", intercept=" + intercept + "]";
	}

}
