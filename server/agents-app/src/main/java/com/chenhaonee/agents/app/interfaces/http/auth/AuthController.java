package com.chenhaonee.agents.app.interfaces.http.auth;

import com.chenhaonee.agents.app.application.auth.AuthTokenApplicationService;
import com.chenhaonee.agents.app.infrastructure.security.AuthProperties;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.domain.auth.TokenAlreadyInitializedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "API Token 认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthTokenApplicationService authTokenApplicationService;
    private final AuthProperties authProperties;

    @Operation(summary = "初始化 API Token（仅首次调用有效）")
    @PostMapping("/init")
    public Response<AuthTokenDto> init(@RequestBody InitTokenRequest request) {
        if (!authProperties.isAllowHttpInit()) {
            return Response.error(403, "HTTP token initialization is disabled");
        }
        try {
            String token = authTokenApplicationService.initializeFromHttp(secretOf(request));
            return Response.success(new AuthTokenDto(token));
        } catch (IllegalArgumentException | IllegalStateException | TokenAlreadyInitializedException e) {
            return Response.error(400, e.getMessage());
        }
    }

    @Operation(summary = "轮换 API Token（需要当前有效 token）")
    @PostMapping("/rotate")
    public Response<AuthTokenDto> rotate(@RequestBody RotateTokenRequest request) {
        try {
            String token = authTokenApplicationService.rotate(newSecretOf(request));
            return Response.success(new AuthTokenDto(token));
        } catch (IllegalArgumentException e) {
            return Response.error(400, e.getMessage());
        } catch (Exception e) {
            return Response.error(500, e.getMessage());
        }
    }

    @Operation(summary = "验证 Token 是否有效")
    @GetMapping("/validate")
    public Response<TokenValidationDto> validate(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractToken(authorization);
        boolean valid = authTokenApplicationService.validate(token);
        return Response.success(new TokenValidationDto(valid));
    }

    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization != null ? authorization : "";
    }

    private String secretOf(InitTokenRequest request) {
        if (request == null) {
            return null;
        }
        return request.secret();
    }

    private String newSecretOf(RotateTokenRequest request) {
        if (request == null) {
            return null;
        }
        return request.newSecret();
    }

    public record InitTokenRequest(String secret) {}

    public record RotateTokenRequest(String newSecret) {}

    public record AuthTokenDto(String token) {}

    public record TokenValidationDto(boolean valid) {}
}
