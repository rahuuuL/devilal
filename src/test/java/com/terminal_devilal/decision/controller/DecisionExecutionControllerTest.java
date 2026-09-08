package com.terminal_devilal.decision.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terminal_devilal.decision.api.DecisionRequests;
import com.terminal_devilal.decision.api.DecisionResponses;
import com.terminal_devilal.decision.service.DecisionExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class DecisionExecutionControllerTest {
    private final DecisionExecutionService service = mock(DecisionExecutionService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new DecisionExecutionController(service)).build();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void evaluatesAnExplicitSubjectSetThroughThePublicApi() throws Exception {
        var request = new DecisionRequests.EvaluationRequest(7L, "HIGH_VOLUME_SCREEN", LocalDate.of(2026, 9, 6), "TICKER",
                List.of(new DecisionRequests.SubjectRequest("TICKER", "RELIANCE", null, Map.of("RVOL", 4.1))));
        var response = new DecisionResponses.EvaluationResponse("HIGH_VOLUME_SCREEN", request.asOfDate(), List.of());
        when(service.evaluate(any())).thenReturn(response);

        mvc.perform(post("/api/decision/evaluate").contentType(APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileCode").value("HIGH_VOLUME_SCREEN"));
        verify(service).evaluate(any());
    }
}
