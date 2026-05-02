package com.chenhaonee.agents.app.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chenhaonee.agents.AgentsApplication;
import com.chenhaonee.agents.app.interfaces.http.auth.AuthController;
import com.chenhaonee.agents.domain.auth.repository.ApiTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = AgentsApplication.class, properties = {
        "agents.auth.allow-http-init=true",
        "agents.auth.token-min-length=1"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {

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
    void shouldRejectProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectProtectedEndpointWithInvalidToken() throws Exception {
        initToken("valid-token");

        mockMvc.perform(get("/api/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldAllowProtectedEndpointWithValidToken() throws Exception {
        initToken("valid-token");

        mockMvc.perform(get("/api/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldRequireCurrentTokenForRotate() throws Exception {
        initToken("old-token");

        mockMvc.perform(post("/api/auth/rotate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.RotateTokenRequest("new-token"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/rotate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.RotateTokenRequest("new-token"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/rotate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer old-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.RotateTokenRequest("new-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("new-token"));

        mockMvc.perform(get("/api/auth/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer old-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false));

        mockMvc.perform(get("/api/auth/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer new-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    private void initToken(String token) throws Exception {
        mockMvc.perform(post("/api/auth/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthController.InitTokenRequest(token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldAllowLocalMcpEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/sse").with(remoteAddr("127.0.0.1")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
    }

    @Test
    void shouldRejectRemoteMcpEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/sse").with(remoteAddr("203.0.113.10")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectMcpEndpointWhenHostIsNotLoopback() throws Exception {
        mockMvc.perform(get("/sse")
                        .with(remoteAddr("127.0.0.1"))
                        .with(serverName("agents.example.com")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private static RequestPostProcessor remoteAddr(String remoteAddr) {
        return request -> {
            request.setRemoteAddr(remoteAddr);
            return request;
        };
    }

    private static RequestPostProcessor serverName(String serverName) {
        return request -> {
            request.setServerName(serverName);
            return request;
        };
    }
}
