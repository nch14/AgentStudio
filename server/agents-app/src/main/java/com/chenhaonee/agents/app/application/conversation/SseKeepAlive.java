package com.chenhaonee.agents.app.application.conversation;

import java.time.Duration;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 给 SSE Flux 挂周期性 {@code :keepalive} 注释行，避免工具执行等无事件静默期里
 * 链路被反向代理判定 idle 而中断（浏览器侧表现为 ERR_HTTP2_PROTOCOL_ERROR）。
 * <p>
 * 注释行不会触发浏览器 EventSource 的 onmessage / 事件回调，仅作为 TCP 层活跃信号。
 */
public final class SseKeepAlive {

    /** 默认心跳间隔；保守留 4× 余量于 nginx 默认 60s 的 proxy_read_timeout。 */
    public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(15);

    private SseKeepAlive() {
    }

    public static Flux<ServerSentEvent<String>> wrap(Flux<ServerSentEvent<String>> source) {
        return wrap(source, DEFAULT_INTERVAL);
    }

    /**
     * 把 source 与一个周期性 keepalive heartbeat merge 起来，heartbeat 严格跟随 source
     * 的生命周期：source onComplete / onError / cancel 之后，heartbeat 立即停止。
     */
    public static Flux<ServerSentEvent<String>> wrap(Flux<ServerSentEvent<String>> source, Duration interval) {
        Sinks.Empty<Void> done = Sinks.empty();
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(interval, interval)
                .map(tick -> ServerSentEvent.<String>builder().comment("keepalive").build())
                .takeUntilOther(done.asMono());
        return source
                .doFinally(signal -> done.tryEmitEmpty())
                .mergeWith(heartbeat);
    }
}
