package com.chenhaonee.agents.domain.auth.repository;

import com.chenhaonee.agents.domain.auth.model.ApiToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    Optional<ApiToken> findFirstByValidTrue();
}
