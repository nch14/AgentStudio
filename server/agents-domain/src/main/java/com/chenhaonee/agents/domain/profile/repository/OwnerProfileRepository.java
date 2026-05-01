package com.chenhaonee.agents.domain.profile.repository;

import com.chenhaonee.agents.domain.profile.model.OwnerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 单实例拥有者资料仓储。
 */
public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, String> {

    Optional<OwnerProfile> findFirstByOrderByIdAsc();
}
