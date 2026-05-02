package com.chenhaonee.agents.app.infrastructure.security;

import com.chenhaonee.agents.domain.auth.service.ApiTokenDomainService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final RequestMatcher LOCAL_MCP_REQUEST = request -> isMcpEndpoint(request)
            && isLocalRequest(request)
            && isLoopbackHost(request);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiTokenDomainService apiTokenDomainService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(FormLoginConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/init").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/validate").permitAll()
                        .requestMatchers(LOCAL_MCP_REQUEST).permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new BearerTokenFilter(apiTokenDomainService), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"code\":401,\"errorMessage\":\"未提供有效的认证信息\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"code\":403,\"errorMessage\":\"无访问权限\"}");
                        })
                );
        return http.build();
    }

    private static boolean isMcpEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return "/sse".equals(path)
                || "/mcp".equals(path)
                || path.startsWith("/mcp/");
    }

    private static boolean isLocalRequest(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return "127.0.0.1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                || "::1".equals(remoteAddr)
                || "localhost".equalsIgnoreCase(remoteAddr);
    }

    private static boolean isLoopbackHost(HttpServletRequest request) {
        String serverName = request.getServerName();
        return "127.0.0.1".equals(serverName)
                || "0:0:0:0:0:0:0:1".equals(serverName)
                || "::1".equals(serverName)
                || "localhost".equalsIgnoreCase(serverName);
    }
}
