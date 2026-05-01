package com.chenhaonee.agents.app.application.coordination;

import com.chenhaonee.agents.domain.coordination.model.Answer;
import com.chenhaonee.agents.domain.coordination.model.Instruction;
import com.chenhaonee.agents.domain.coordination.model.Question;
import com.chenhaonee.agents.domain.coordination.model.Questions;
import com.chenhaonee.agents.domain.coordination.service.InstructionDomainService;
import com.chenhaonee.agents.domain.coordination.service.QuestionsDomainService;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.service.TaskDomainService;
import com.chenhaonee.agents.domain.task.service.TaskTurnDomainService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 协同命令应用服务。
 */
@Service
@RequiredArgsConstructor
public class CoordinationCommandApplicationService {

    private final TaskDomainService taskDomainService;
    private final QuestionsDomainService questionsDomainService;
    private final InstructionDomainService instructionDomainService;
    private final TaskTurnDomainService taskTurnDomainService;

    @Transactional(rollbackFor = Exception.class)
    public Questions resolveQuestions(String taskCode, String questionsCode, List<Answer> answers) {
        taskDomainService.requireTask(taskCode);
        Questions resolved = questionsDomainService.resolveQuestions(taskCode, questionsCode, answers);

        // 该 turn 下所有 questions 都答完才恢复 RUNNING
        if (!questionsDomainService.hasPendingQuestions(resolved.getTurnCode())) {
            TaskTurn turn = taskTurnDomainService.requireTurn(resolved.getTurnCode());
            turn.resumeRunning();
            taskTurnDomainService.save(turn);
        }

        return resolved;
    }

    @Transactional(rollbackFor = Exception.class)
    public Instruction createInstruction(String taskCode, String turnCode, String content) {
        taskDomainService.requireTask(taskCode);
        return instructionDomainService.createInstruction(taskCode, turnCode, content);
    }

    @Transactional(rollbackFor = Exception.class)
    public void acceptInstruction(String instructionCode) {
        instructionDomainService.acceptInstruction(instructionCode);
    }
}
