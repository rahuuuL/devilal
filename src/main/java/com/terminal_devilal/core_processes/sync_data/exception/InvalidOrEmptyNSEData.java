package com.terminal_devilal.core_processes.sync_data.exception;

@SuppressWarnings("serial")
public class InvalidOrEmptyNSEData extends RuntimeException{
	
    public InvalidOrEmptyNSEData(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InvalidOrEmptyNSEData(String message) {
        super(message);
    }

}
