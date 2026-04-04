package com.example.rag.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.rag.orchestrator.ChatOrchestratorService;


@WebMvcTest(RagController.class)
class RagControllerTestBkp {

  /*  @Autowired
    private MockMvc mockMvc;*/

    @MockBean
    private ChatOrchestratorService orchestrator;

    @Test
    void shouldReturnResponse() throws Exception {
        // test logic
    }
}
