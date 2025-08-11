package com.terminal_devilal.business_tools.beta.dto;

public class BetaDTO {
	
	private double betaWRTAtoB;
	private double betaWRTBtoA;
	public BetaDTO(double betaWRTAtoB, double betaWRTBtoA) {
		super();
		this.betaWRTAtoB = betaWRTAtoB;
		this.betaWRTBtoA = betaWRTBtoA;
	}
	public double getBetaWRTAtoB() {
		return betaWRTAtoB;
	}
	public void setBetaWRTAtoB(double betaWRTAtoB) {
		this.betaWRTAtoB = betaWRTAtoB;
	}
	public double getBetaWRTBtoA() {
		return betaWRTBtoA;
	}
	public void setBetaWRTBtoA(double betaWRTBtoA) {
		this.betaWRTBtoA = betaWRTBtoA;
	}
	
	

}
