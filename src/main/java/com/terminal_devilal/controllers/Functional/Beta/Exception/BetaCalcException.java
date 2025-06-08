package com.terminal_devilal.controllers.Functional.Beta.Exception;

@SuppressWarnings("serial")
public class BetaCalcException extends RuntimeException {

	public BetaCalcException(String message, Throwable cause) {
		super(message, cause);
	}

	public BetaCalcException(String message) {
		super(message);
	}
}
