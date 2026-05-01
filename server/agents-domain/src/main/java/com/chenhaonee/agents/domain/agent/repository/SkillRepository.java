package com.chenhaonee.agents.domain.agent.repository;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.agent.model.Skill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Skill 资产仓储。
 */
public interface SkillRepository extends JpaRepository<Skill, String> {

    Optional<Skill> findByCode(String code);

    List<Skill> findByStatus(Status status);
}
