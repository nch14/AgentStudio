package com.chenhaonee.agents.app.interfaces.http.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chenhaonee.agents.AgentsApplication;
import com.chenhaonee.agents.domain.auth.repository.ApiTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AgentsApplication.class, properties = "agents.auth.token-min-length=1")
@AutoConfigureMockMvc
class AuthHttpInitDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiTokenRepository apiTokenRepository;

    @BeforeEach
    void setUp() {
        apiTokenRepository.deleteAll();
    }

    @Test
    void shouldRejectHttpInitWhenDisabledByDefault() throws Exception {
        mockMvc.perform(post("/api/auth/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.InitTokenRequest("initial-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(403));
    }
}
