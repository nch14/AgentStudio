package com.chenhaonee.agents.app.application.conversation;

import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 可取消的流桥接类。
 *
 * <p>将 raw Flux 转发到内部 Sinks.Many，提供 {@link #connect()} 方法显式启动上游订阅，
 * 并提供 {@link #cancel()} 方法向所有下游注入 {@code interrupted} 事件后 complete。
 * 支持多订阅者（replay 256 缓冲），保证 cancel() 幂等。</p>
 *
 * <p>上游 emit（I/O 线程）与 {@link #cancel()}（HTTP servlet 线程）可能并发触达
 * sink。{@code Sinks.many().replay()} 默认不是序列化 sink，因此所有 emit 操作
 * 统一经过 {@code emitLock} 串行化，避免事件被静默丢弃。</p>
 */
public class CancellableStream {

    private static final Logger log = LoggerFactory.getLogger(CancellableStream.class);
    private static final MessagesEvent INTERRUPTED_EVENT = new MessagesEvent("interrupted", "{}");

    private final Sinks.Many<MessagesEvent> sink = Sinks.many().replay().limit(256);
    private final Flux<MessagesEvent> source;
    private final AtomicBoolean done = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicReference<Disposable> upstream = new AtomicReference<>();
    private final Object emitLock = new Object();

    public CancellableStream(Flux<MessagesEvent> source) {
        this.source = Objects.requireNonNull(source, "source must not be null");
    }

    /**
     * 启动上游订阅。调用方应先完成下游订阅和 registry 注册，再调用本方法。
     *
     * @return true 表示本次调用启动了订阅；false 表示已启动或已取消
     */
    public boolean connect() {
        if (!connected.compareAndSet(false, true)) {
            return false;
        }
        if (done.get()) {
            return false;
        }
        Disposable subscription = source.subscribe(
                event -> {
                    if (!done.get()) {
                        emitNext(event);
                    }
                },
                error -> {
                    if (done.compareAndSet(false, true)) {
                        emitError(error);
                    }
                },
                () -> {
                    if (done.compareAndSet(false, true)) {
                        emitComplete();
                    }
                }
        );
        upstream.set(subscription);
        if (done.get()) {
            subscription.dispose();
        }
        return true;
    }

    /**
     * 返回广播 Flux，支持多订阅者接续（replay 256 缓冲）。
     */
    public Flux<MessagesEvent> asFlux() {
        return sink.asFlux();
    }

    /**
     * 幂等取消：向流注入 interrupted 事件并 complete。
     *
     * @return true 表示本次调用触发了取消；false 表示流已自然结束或已被取消
     */
    public boolean cancel() {
        if (done.compareAndSet(false, true)) {
            emitNext(INTERRUPTED_EVENT);
            emitComplete();
            Disposable subscription = upstream.getAndSet(null);
            if (subscription != null) {
                subscription.dispose();
            }
            return true;
        }
        return false;
    }

    private void emitNext(MessagesEvent event) {
        synchronized (emitLock) {
            Sinks.EmitResult result = sink.tryEmitNext(event);
            if (shouldWarn(result)) {
                log.warn("messages event emit failed: result={}, eventType={}", result, event.eventType());
            }
        }
    }

    private void emitComplete() {
        synchronized (emitLock) {
            Sinks.EmitResult result = sink.tryEmitComplete();
            if (shouldWarn(result)) {
                log.warn("messages stream complete emit failed: result={}", result);
            }
        }
    }

    private void emitError(Throwable error) {
        synchronized (emitLock) {
            Sinks.EmitResult result = sink.tryEmitError(error);
            if (shouldWarn(result)) {
                log.warn("messages stream error emit failed: result={}", result, error);
            }
        }
    }

    private boolean shouldWarn(Sinks.EmitResult result) {
        return result != Sinks.EmitResult.OK
                && result != Sinks.EmitResult.FAIL_TERMINATED
                && result != Sinks.EmitResult.FAIL_CANCELLED;
    }
}
