package com.terminal_devilal.decision.controller;

import com.terminal_devilal.decision.exception.DecisionException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class DecisionExceptionHandler {
    @ExceptionHandler(DecisionException.class) public ResponseEntity<Map<String,String>> decision(DecisionException e){return ResponseEntity.badRequest().body(Map.of("code",e.getCode(),"message",e.getMessage()));}
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class}) public ResponseEntity<Map<String,String>> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST","message",e.getMessage()==null?"Invalid request":e.getMessage()));}
}
