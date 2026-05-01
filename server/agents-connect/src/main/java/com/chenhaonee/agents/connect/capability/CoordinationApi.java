package com.chenhaonee.agents.connect.capability;

import java.util.List;

/**
 * Agent 可调用的协同管理能力。
 */
public interface CoordinationApi {

    /**
     * Agent 向用户发起一组提问
     */
    String askQuestions(String turnCode, List<QuestionDto> questions);

    /**
     * Agent 加载用户推送的指令
     */
    List<InstructionDto> loadInstruction(String taskCode);

    /**
     * Agent 确认已收到指令并标记为 ACCEPTED
     */
    void acceptInstruction(String instructionCode);

    /**
     * Agent 查询对应问题的回答
     */
    List<AnswerDto> fetchAnswers(String questionsCode);

    /**
     * 判断给定 turnCode 是否存在尚未回答的 pending questions
     */
    boolean hasPendingQuestions(String turnCode);

    record QuestionDto(
            String code,
            String text,
            List<String> options
    ) {
    }

    record InstructionDto(
            String code,
            String content
    ) {
    }

    record AnswerDto(
            String questionCode,
            String selectedOption,
            String userInput
    ) {
    }
}
