package com.chenhaonee.agents.app.interfaces.http.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest(classes = AgentsApplication.class, properties = {
        "agents.auth.allow-http-init=true",
        "agents.auth.token-min-length=1"
})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

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
    void shouldInitToken() throws Exception {
        mockMvc.perform(post("/api/auth/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.InitTokenRequest("my-secret-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("my-secret-token"));
    }

    @Test
    void shouldRejectSecondInit() throws Exception {
        mockMvc.perform(post("/api/auth/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.InitTokenRequest("first-token"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.InitTokenRequest("second-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldValidateToken() throws Exception {
        mockMvc.perform(post("/api/auth/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.InitTokenRequest("valid-token"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true));

        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer wrong-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false));
    }

}
