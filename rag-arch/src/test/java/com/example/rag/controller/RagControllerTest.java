package com.example.rag.controller;

/*import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;*/

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.rag.model.ChatResponse;
import com.example.rag.orchestrator.ChatOrchestratorService;

@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatOrchestratorService orchestrator;

    @Test
    void shouldReturnChatResponse() throws Exception {

        ChatResponse response =
                new ChatResponse("answer", List.of("doc.pdf"));

        Mockito.when(orchestrator.handleUserQuery(any()))
                .thenReturn(response);

        mockMvc.perform(post("/rag/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
