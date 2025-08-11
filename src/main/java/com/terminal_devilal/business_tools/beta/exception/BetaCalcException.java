package com.terminal_devilal.business_tools.beta.exception;

@SuppressWarnings("serial")
public class BetaCalcException extends RuntimeException {

	public BetaCalcException(String message, Throwable cause) {
		super(message, cause);
	}

	public BetaCalcException(String message) {
		super(message);
	}
}
