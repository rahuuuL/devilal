package com.terminal_devilal.business_tools.mannkendall.exception;

public class InvalidColumnException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidColumnException(String columnName) {
		super("Invalid column requested for analysis: " + columnName);
	}
}