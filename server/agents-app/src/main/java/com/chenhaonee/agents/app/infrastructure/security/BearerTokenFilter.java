package com.chenhaonee.agents.app.infrastructure.security;

import com.chenhaonee.agents.domain.auth.service.ApiTokenDomainService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class BearerTokenFilter extends OncePerRequestFilter {

    private final ApiTokenDomainService apiTokenDomainService;

    public BearerTokenFilter(ApiTokenDomainService apiTokenDomainService) {
        this.apiTokenDomainService = apiTokenDomainService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (apiTokenDomainService.validate(token)) {
                SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated("owner", token, java.util.List.of()));
            }
        }
        filterChain.doFilter(request, response);
    }
}
