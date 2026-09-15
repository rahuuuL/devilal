package com.terminal_devilal.decision.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.terminal_devilal.decision.api.DecisionRequests.EvaluationRequest;
import com.terminal_devilal.decision.api.DecisionResponses.EvaluationResponse;
import com.terminal_devilal.decision.service.DecisionExecutionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/decision")
public class DecisionExecutionController {
	private final DecisionExecutionService service;

	public DecisionExecutionController(DecisionExecutionService service) {
		this.service = service;
	}

	@PostMapping("/evaluate")
	public EvaluationResponse evaluate(@Valid @RequestBody EvaluationRequest request) {
		return service.evaluate(request);
	}

	@PostMapping("/test-rule")
	public EvaluationResponse testRule(@Valid @RequestBody EvaluationRequest request) {
		return service.evaluate(request);
	}
}
