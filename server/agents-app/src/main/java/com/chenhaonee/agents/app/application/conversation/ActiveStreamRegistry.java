package com.chenhaonee.agents.app.application.conversation;

import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 管理当前正在生成中的会话流。
 * 用于支持客户端在切换回会话时接续正在进行的流，以及主动中断对话。
 */
@Component
public class ActiveStreamRegistry {

    private record ActiveEntry(Flux<MessagesEvent> stream, Runnable cancelAction) {
    }

    private final ConcurrentHashMap<String, ActiveEntry> activeStreams = new ConcurrentHashMap<>();

    /**
     * 注册一个活跃的会话流及其取消动作。
     *
     * <p>调用方负责在流终止时调用 {@link #remove(String)} 清理；本方法不再用
     * {@code doFinally} 自动清理，因为 {@code doFinally} 只在 stream 被订阅时
     * 生效，而 {@code streamResume} 可能从不被调用，导致 map 泄漏。</p>
     */
    public void register(String sessionCode, Flux<MessagesEvent> stream, Runnable cancelAction) {
        activeStreams.put(sessionCode, new ActiveEntry(stream, cancelAction));
    }

    /**
     * 显式移除指定 sessionCode 的活跃流（幂等）。
     * 调用方应在流自然终止时调用以避免 map 泄漏。
     */
    public void remove(String sessionCode) {
        activeStreams.remove(sessionCode);
    }

    /**
     * 获取指定会话的活跃流（如果有）。
     */
    public Optional<Flux<MessagesEvent>> get(String sessionCode) {
        ActiveEntry entry = activeStreams.get(sessionCode);
        return entry == null ? Optional.empty() : Optional.of(entry.stream());
    }

    /**
     * 取消指定会话的活跃流。
     *
     * @return true 表示找到并触发了取消；false 表示无活跃流
     */
    public boolean cancel(String sessionCode) {
        ActiveEntry entry = activeStreams.remove(sessionCode);
        if (entry == null) {
            return false;
        }
        entry.cancelAction().run();
        return true;
    }
}
