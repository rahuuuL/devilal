package com.terminal_devilal.core_processes.nse_cookie_mng.exception;

@SuppressWarnings("serial")
public class CookieSaveException extends RuntimeException {
    public CookieSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}