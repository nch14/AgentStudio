package com.chenhaonee.agents.app.application.coordination;

import com.chenhaonee.agents.domain.coordination.model.Instruction;
import com.chenhaonee.agents.domain.coordination.model.Questions;
import com.chenhaonee.agents.domain.coordination.repository.InstructionRepository;
import com.chenhaonee.agents.domain.coordination.repository.QuestionsRepository;
import com.chenhaonee.agents.domain.coordination.service.QuestionsDomainService;
import com.chenhaonee.agents.domain.task.service.TaskDomainService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 协同查询应用服务。
 */
@Service
@RequiredArgsConstructor
public class CoordinationQueryApplicationService {

    private final TaskDomainService taskDomainService;
    private final QuestionsRepository questionsRepository;
    private final InstructionRepository instructionRepository;

    public List<Instruction> listInstructions(String taskCode) {
        taskDomainService.requireTask(taskCode);
        return instructionRepository.findByTaskCodeOrderByCreatedAtAsc(taskCode);
    }

    public List<Questions> listQuestions(String taskCode, Boolean resolved) {
        taskDomainService.requireTask(taskCode);
        if (resolved != null) {
            return questionsRepository.findByTaskCodeAndResolvedOrderByOpenedAtDesc(taskCode, resolved);
        }
        return questionsRepository.findByTaskCodeOrderByOpenedAtDesc(taskCode);
    }

    public Questions getQuestionsByCode(String questionsCode) {
        return questionsRepository.findByCode(questionsCode)
                .orElseThrow(() -> new java.util.NoSuchElementException("questions not found: " + questionsCode));
    }
}
