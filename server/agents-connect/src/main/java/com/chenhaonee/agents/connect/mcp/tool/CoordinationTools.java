package com.chenhaonee.agents.connect.mcp.tool;

import com.alibaba.fastjson2.JSON;
import com.chenhaonee.agents.connect.capability.CoordinationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 提供给大语言模型的协同工具集。
 */
@Component
@RequiredArgsConstructor
public class CoordinationTools {

    private final CoordinationApi coordinationApi;

    @Tool(name = "askQuestions", description = "当需要向用户提问以获取补充信息时调用此工具，调用后任务将暂停执行并等待用户回复。传入的问题列表应为JSON格式数组。")
    public String askQuestions(
            @ToolParam(description = "必填项，当前对话轮次的标识符 turnCode") String turnCode,
            @ToolParam(description = "必填项，问题列表的JSON数组序列化字符串。"
                    + "格式：[{\"text\":\"问题正文（必填，不可省略）\",\"options\":[\"选项A\",\"选项B\"]}]。"
                    + "没有选项时 options 传空数组 []。options 元素必须是纯字符串，不可传对象。") String questionsJson) {
        List<CoordinationApi.QuestionDto> questions = StringUtils.hasText(questionsJson)
                ? JSON.parseArray(questionsJson, CoordinationApi.QuestionDto.class)
                : Collections.emptyList();
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("questions array cannot be empty");
        }
        for (int i = 0; i < questions.size(); i++) {
            if (!StringUtils.hasText(questions.get(i).text())) {
                throw new IllegalArgumentException("question[" + i + "].text is required");
            }
        }
        return coordinationApi.askQuestions(turnCode, questions);
    }

    @Tool(name = "loadInstruction", description = "获取用户主动推送或补充给Agent的指令信息。")
    public String loadInstruction(
            @ToolParam(description = "必填项，关联的任务编码 taskCode") String taskCode) {
        List<CoordinationApi.InstructionDto> instructions = coordinationApi.loadInstruction(taskCode);
        return JSON.toJSONString(instructions);
    }
}
