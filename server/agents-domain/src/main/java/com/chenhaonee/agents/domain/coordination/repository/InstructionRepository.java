package com.chenhaonee.agents.domain.coordination.repository;

import com.chenhaonee.agents.domain.coordination.model.Instruction;
import com.chenhaonee.agents.domain.coordination.model.InstructionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstructionRepository extends JpaRepository<Instruction, Long> {

    List<Instruction> findByTaskCodeAndStatusOrderByCreatedAtAsc(String taskCode, InstructionStatus status);

    List<Instruction> findByTaskCodeOrderByCreatedAtAsc(String taskCode);

    Optional<Instruction> findByCode(String code);
}
