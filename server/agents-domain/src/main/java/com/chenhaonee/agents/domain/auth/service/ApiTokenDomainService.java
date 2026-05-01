package com.chenhaonee.agents.domain.auth.service;

import com.chenhaonee.agents.domain.auth.TokenAlreadyInitializedException;
import com.chenhaonee.agents.domain.auth.model.ApiToken;
import com.chenhaonee.agents.domain.auth.repository.ApiTokenRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ApiTokenDomainService {

    private final ApiTokenRepository apiTokenRepository;
    private final PasswordEncoder passwordEncoder;

    private volatile String cachedBcryptHash = null;
    private volatile String cachedPlainToken = null;

    public ApiTokenDomainService(ApiTokenRepository apiTokenRepository,
                                 PasswordEncoder passwordEncoder) {
        this.apiTokenRepository = apiTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiToken initialize(String plainToken) {
        requireTokenText(plainToken);
        Optional<ApiToken> existing = apiTokenRepository.findFirstByValidTrue();
        if (existing.isPresent()) {
            throw new TokenAlreadyInitializedException();
        }
        ApiToken token = new ApiToken(passwordEncoder.encode(plainToken));
        ApiToken saved = apiTokenRepository.save(token);
        cachedBcryptHash = saved.getTokenHash();
        cachedPlainToken = null;
        return saved;
    }

    public boolean validate(String plainToken) {
        if (!StringUtils.hasText(plainToken)) {
            return false;
        }
        if (plainToken.equals(cachedPlainToken)) {
            return true;
        }
        String bcryptHash = loadCachedHash();
        if (bcryptHash == null) {
            return false;
        }
        boolean matches = passwordEncoder.matches(plainToken, bcryptHash);
        if (matches) {
            cachedPlainToken = plainToken;
        }
        return matches;
    }

    public boolean hasValidToken() {
        if (cachedBcryptHash != null) {
            return true;
        }
        return apiTokenRepository.findFirstByValidTrue().isPresent();
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiToken rotate(String plainToken) {
        requireTokenText(plainToken);
        String hash = passwordEncoder.encode(plainToken);
        Optional<ApiToken> current = apiTokenRepository.findFirstByValidTrue();
        current.ifPresent(t -> {
            t.setValid(false);
            apiTokenRepository.save(t);
        });
        ApiToken newToken = new ApiToken(hash);
        ApiToken saved = apiTokenRepository.save(newToken);
        cachedBcryptHash = hash;
        cachedPlainToken = null;
        return saved;
    }

    private String loadCachedHash() {
        if (cachedBcryptHash != null) {
            return cachedBcryptHash;
        }
        Optional<ApiToken> token = apiTokenRepository.findFirstByValidTrue();
        token.ifPresent(t -> cachedBcryptHash = t.getTokenHash());
        return cachedBcryptHash;
    }

    private void requireTokenText(String plainToken) {
        if (!StringUtils.hasText(plainToken)) {
            throw new IllegalArgumentException("API token must not be blank");
        }
    }
}
