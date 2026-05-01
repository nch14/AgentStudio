package com.chenhaonee.agents.domain.coordination.service;

import com.chenhaonee.agents.domain.coordination.model.Instruction;
import com.chenhaonee.agents.domain.coordination.model.InstructionStatus;
import com.chenhaonee.agents.domain.coordination.repository.InstructionRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstructionDomainService {

    private final InstructionRepository instructionRepository;

    public InstructionDomainService(InstructionRepository instructionRepository) {
        this.instructionRepository = instructionRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Instruction createInstruction(String taskCode, String turnCode, String content) {
        Instruction entity = new Instruction();
        entity.initialize(taskCode, turnCode, content);
        return instructionRepository.save(entity);
    }

    public List<Instruction> loadPendingInstructions(String taskCode) {
        return instructionRepository.findByTaskCodeAndStatusOrderByCreatedAtAsc(taskCode, InstructionStatus.PENDING);
    }

    @Transactional(rollbackFor = Exception.class)
    public void acceptInstruction(String code) {
        Instruction instruction = instructionRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("instruction not found: " + code));
        instruction.accept();
        instructionRepository.save(instruction);
    }
}
