package com.terminal_devilal.decision.controller;

import com.terminal_devilal.decision.api.DecisionRequests.IndicatorPatchRequest;
import com.terminal_devilal.decision.api.DecisionRequests.IndicatorRequest;
import com.terminal_devilal.decision.api.DecisionRequests.IndicatorParameterRequest;
import com.terminal_devilal.decision.api.DecisionResponses.IndicatorParameterResponse;
import com.terminal_devilal.decision.api.DecisionResponses.IndicatorResponse;
import com.terminal_devilal.decision.service.DecisionIndicatorService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/decision/indicators")
public class DecisionIndicatorController {
    private final DecisionIndicatorService service;
    public DecisionIndicatorController(DecisionIndicatorService service){this.service=service;}
    @GetMapping public List<IndicatorResponse> list(@RequestParam(required=false) String subjectType){return service.list(subjectType);}
    @GetMapping("/{code}") public IndicatorResponse get(@PathVariable String code){return service.get(code);}
    @GetMapping("/{code}/parameters") public List<IndicatorParameterResponse> parameters(@PathVariable String code){return service.parameters(code);}
    @PostMapping("/{code}/parameters") public ResponseEntity<IndicatorParameterResponse> createParameter(@PathVariable String code,@Valid @RequestBody IndicatorParameterRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.createParameter(code,request));}
    @PutMapping("/{code}/parameters/{parameterCode}") public IndicatorParameterResponse updateParameter(@PathVariable String code,@PathVariable String parameterCode,@Valid @RequestBody IndicatorParameterRequest request){return service.upsertParameter(code,parameterCode,request);}
    @PostMapping public ResponseEntity<IndicatorResponse> create(@Valid @RequestBody IndicatorRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));}
    @PutMapping("/{code}") public IndicatorResponse update(@PathVariable String code,@Valid @RequestBody IndicatorRequest request){if(!code.equals(request.code()))throw new IllegalArgumentException("Path code and body code differ");return service.save(request);}
    @PatchMapping("/{code}") public IndicatorResponse patch(@PathVariable String code,@RequestBody IndicatorPatchRequest request){return service.patch(code,request);}
    @PatchMapping("/{code}/enable") public IndicatorResponse enable(@PathVariable String code,@RequestParam boolean enabled){return service.setEnabled(code,enabled);}
}
