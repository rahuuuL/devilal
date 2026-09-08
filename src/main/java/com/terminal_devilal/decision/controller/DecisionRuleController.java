package com.terminal_devilal.decision.controller;

import com.terminal_devilal.decision.api.DecisionRequests.RuleRequest;
import com.terminal_devilal.decision.api.DecisionResponses.*;
import com.terminal_devilal.decision.service.DecisionRuleService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/decision/rules")
public class DecisionRuleController {
    private final DecisionRuleService service;
    public DecisionRuleController(DecisionRuleService service){this.service=service;}
    @GetMapping public List<RuleResponse> list(@RequestParam Long ownerId){return service.list(ownerId);}
    @GetMapping("/{id}") public RuleResponse get(@PathVariable UUID id){return service.get(id);}
    @PostMapping public ResponseEntity<RuleResponse> create(@Valid @RequestBody RuleRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));}
    @PutMapping("/{id}") public RuleResponse update(@PathVariable UUID id,@Valid @RequestBody RuleRequest request){return service.update(id,request);}
    @PostMapping("/{id}/validate") public RuleResponse validate(@PathVariable UUID id){return service.validate(id);}
    @PostMapping("/{id}/activate") public RuleResponse activate(@PathVariable UUID id){return service.activate(id);}
    @PostMapping("/{id}/disable") public RuleResponse disable(@PathVariable UUID id){return service.disable(id);}
    @GetMapping("/{id}/versions") public List<VersionResponse> versions(@PathVariable UUID id){return service.versions(id);}
    @GetMapping("/{id}/current-version") public VersionResponse currentVersion(@PathVariable UUID id){return service.currentVersionResponse(id);}
}
