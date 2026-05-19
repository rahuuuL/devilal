package com.terminal_devilal.business_tools.mannkendall.dto;

public class MannKendallAPIResponse extends MannKendallResponse {

	private String ticker;

	public MannKendallAPIResponse() {
		super();
	}

	public MannKendallAPIResponse(String ticker, MannKendallResponse response) {
		super();
		this.ticker = ticker;

		// Copy all fields from response to this
		this.setTrend(response.getTrend());
		this.setH(response.getH());
		this.setP(response.getP());
		this.setZ(response.getZ());
		this.setTau(response.getTau());
		this.setS(response.getS());
		this.setVar_s(response.getVar_s());
		this.setSlope(response.getSlope());
		this.setIntercept(response.getIntercept());
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}
}
