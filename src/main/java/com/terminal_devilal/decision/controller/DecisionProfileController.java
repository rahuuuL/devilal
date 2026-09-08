package com.terminal_devilal.decision.controller;

import com.terminal_devilal.decision.api.DecisionRequests.*;
import com.terminal_devilal.decision.api.DecisionResponses.*;
import com.terminal_devilal.decision.service.DecisionProfileService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/decision/profiles")
public class DecisionProfileController {
    private final DecisionProfileService service;
    public DecisionProfileController(DecisionProfileService service){this.service=service;}
    @GetMapping public List<ProfileResponse> list(@RequestParam Long ownerId){return service.list(ownerId);}
    @PostMapping public ResponseEntity<ProfileResponse> create(@Valid @RequestBody ProfileRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));}
    @PutMapping("/{id}") public ProfileResponse update(@PathVariable UUID id,@Valid @RequestBody ProfileRequest request){return service.update(id,request);}
    @GetMapping("/{profileCode}/outputs") public List<OutputResponse> outputs(@PathVariable String profileCode,@RequestParam Long ownerId){return service.outputs(ownerId,profileCode);}
    @PostMapping("/outputs") public ResponseEntity<OutputResponse> addOutput(@Valid @RequestBody OutputRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.addOutput(request));}
}
