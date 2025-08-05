package com.terminal_devilal.controllers.Advice;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.terminal_devilal.controllers.DataGathering.Exception.InvalidColumnException;
import com.terminal_devilal.controllers.DataGathering.Exception.InvalidOrEmptyNSEData;
import com.terminal_devilal.controllers.Functional.Beta.Exception.BetaCalcException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IOException.class)
	public ResponseEntity<Object> handleIOException(IOException ex) {

		if ("Received RST_STREAM: Internal error".equals(ex.getMessage())) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
		}

		// Fallback for other IOExceptions
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("An I/O error occurred: " + ex.getMessage());
	}

	@ExceptionHandler(InterruptedException.class)
	public ResponseEntity<String> handleInterruptedException(InterruptedException ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Exception occured due to a timeout, cancellation or thread interruption");
	}

	@ExceptionHandler(InvalidOrEmptyNSEData.class)
	public void invalidorEmptyNSEData(InvalidOrEmptyNSEData ex) {
		System.out.println(ex.getLocalizedMessage());
	}

	@ExceptionHandler(BetaCalcException.class)
	public ResponseEntity<String> invalidorEmptyNSEData(BetaCalcException ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
	}

	@ExceptionHandler(InvalidColumnException.class)
	public ResponseEntity<String> handleInvalidColumnException(InvalidColumnException ex) {
		String body = ex.getMessage();
		return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
	}

}
