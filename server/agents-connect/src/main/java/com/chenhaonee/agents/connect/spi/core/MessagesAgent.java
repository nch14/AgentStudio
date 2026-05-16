package com.chenhaonee.agents.connect.spi.core;

import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

/**
 * Messages 型 Agent SPI。
 *
 * <p>面向 Anthropic Messages 风格的复杂对话场景，返回标准语义事件流。
 */
public interface MessagesAgent {

    AgentProvider supportedType();

    /**
     * 流式 Messages 调用。
     *
     * @param agentCode Agent 编码
     * @param requestJson 标准 Messages 请求 JSON
     * @param sessionCode AgentSession 的业务标识，一定不为空
     * @return 标准语义事件流
     */
    Flux<MessagesEvent> stream(String agentCode, String requestJson, @NonNull String sessionCode);

    /**
     * 取消 {@code sessionCode} 上正在进行的 turn。
     *
     * <p>实现需尽力终止 provider 侧的思考与工具调用（关闭 HTTP 连接 / 关闭 SDK stream
     * / 终止子进程等）。默认实现返回 false 表示该 provider 不支持取消。</p>
     *
     * @return true 表示找到并触发了取消；false 表示当前 session 无活跃 turn 或不支持
     */
    default boolean cancelStream(@NonNull String sessionCode) {
        return false;
    }
}
