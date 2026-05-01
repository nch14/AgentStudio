package com.chenhaonee.agents.connect.capability.impl;

import com.chenhaonee.agents.connect.capability.CoordinationApi;
import com.chenhaonee.agents.domain.coordination.model.Answer;
import com.chenhaonee.agents.domain.coordination.model.Instruction;
import com.chenhaonee.agents.domain.coordination.model.Question;
import com.chenhaonee.agents.domain.coordination.model.Questions;
import com.chenhaonee.agents.domain.coordination.service.InstructionDomainService;
import com.chenhaonee.agents.domain.coordination.service.QuestionsDomainService;
import com.chenhaonee.agents.domain.notify.service.MessageCenter;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.service.TaskTurnDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CoordinationApi 的 connect 模块实现。
 */
@Component
@RequiredArgsConstructor
public class DefaultCoordinationApi implements CoordinationApi {

    private final QuestionsDomainService questionsDomainService;
    private final InstructionDomainService instructionDomainService;
    private final TaskTurnDomainService taskTurnDomainService;
    private final MessageCenter messageCenter;

    @Value("${app.notify.config-code.questions-raised:questions_raised}")
    private String questionsRaisedConfigCode;

    @Override
    public String askQuestions(String turnCode, List<QuestionDto> questions) {
        TaskTurn taskTurn = taskTurnDomainService.requireTurn(turnCode);
        String taskCode = taskTurn.getTaskCode();

        List<Question> domainQuestions = new java.util.ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            var q = questions.get(i);
            domainQuestions.add(new Question(
                    q.code() != null ? q.code() : "Q" + (i + 1),
                    q.text(),
                    q.options()));
        }

        Questions saved = questionsDomainService.createQuestions(taskCode, turnCode, domainQuestions);

        // 通知用户 Agent 正在请求回答问题
        messageCenter.send(questionsRaisedConfigCode,
                "Agent 请求提问: " + taskCode,
                "Task " + taskCode + " 正等待您的回复。");

        return saved.getCode();
    }

    @Override
    public List<InstructionDto> loadInstruction(String taskCode) {
        List<Instruction> instructions = instructionDomainService.loadPendingInstructions(taskCode);
        return instructions.stream()
                .map(ins -> new InstructionDto(ins.getCode(), ins.getContent()))
                .collect(Collectors.toList());
    }

    @Override
    public void acceptInstruction(String instructionCode) {
        instructionDomainService.acceptInstruction(instructionCode);
    }

    @Override
    public List<AnswerDto> fetchAnswers(String questionsCode) {
        List<Answer> answers = questionsDomainService.fetchAnswers(questionsCode);
        return answers.stream()
                .map(ans -> new AnswerDto(ans.questionCode(), ans.selectedOption(), ans.userInput()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasPendingQuestions(String turnCode) {
        return questionsDomainService.hasPendingQuestions(turnCode);
    }
}
