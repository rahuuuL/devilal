package com.terminal_devilal.decision.exception;

public class DecisionException extends RuntimeException {
    private final String code;
    public DecisionException(String code,String message){super(message);this.code=code;}
    public String getCode(){return code;}
}
