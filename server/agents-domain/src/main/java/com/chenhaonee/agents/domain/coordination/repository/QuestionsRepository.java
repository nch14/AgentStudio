package com.chenhaonee.agents.domain.coordination.repository;

import com.chenhaonee.agents.domain.coordination.model.Questions;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionsRepository extends JpaRepository<Questions, Long> {

    Optional<Questions> findByCode(String code);

    Optional<Questions> findFirstByTaskCodeAndResolvedFalseOrderByOpenedAtDesc(String taskCode);

    List<Questions> findByTaskCodeOrderByOpenedAtDesc(String taskCode);

    List<Questions> findByTaskCodeAndResolvedOrderByOpenedAtDesc(String taskCode, boolean resolved);

    boolean existsByTurnCodeAndResolved(String turnCode, boolean resolved);
}
