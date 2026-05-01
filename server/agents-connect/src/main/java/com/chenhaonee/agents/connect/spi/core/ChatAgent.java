package com.chenhaonee.agents.connect.spi.core;

import com.chenhaonee.agents.connect.spi.model.ConversationChunk;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

/**
 * 对话型 Agent SPI。
 * <p>
 * 面向 OpenAI Chat Completions 等文本型对话场景，支持流式逐 token 返回。
 * impl 层通过注入的 AgentConfigApi 按需查询 agent profile 和配置。
 * assistant 回复的持久化由协议应用服务负责。
 */
public interface ChatAgent {

    AgentProvider supportedType();

    /**
     * 流式对话。
     *
     * @param agentCode         Agent 编码，impl 可据此通过 AgentConfigApi 查询 profile/config
     * @param userInput         本轮用户输入
     * @param sessionCode       AgentSession 的业务标识，一定不为空
     * @return 流式响应内容
     */
    Flux<ConversationChunk> chat(String agentCode, String userInput, @NonNull String sessionCode);
}
