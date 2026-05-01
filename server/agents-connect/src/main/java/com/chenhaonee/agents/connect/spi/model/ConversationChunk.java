package com.chenhaonee.agents.connect.spi.model;

import java.util.Objects;

/**
 * 文本型对话流式响应单元。
 */
public record ConversationChunk(
        ChunkType type,
        String content,
        String sessionCode
) {

    public ConversationChunk {
        Objects.requireNonNull(type, "type cannot be null");
    }

    public enum ChunkType {
        TEXT,
        DONE
    }

    public static ConversationChunk text(String content) {
        return new ConversationChunk(ChunkType.TEXT, content, null);
    }

    public static ConversationChunk done() {
        return new ConversationChunk(ChunkType.DONE, "", null);
    }

    public ConversationChunk withSessionCode(String sessionCode) {
        return new ConversationChunk(type, content, sessionCode);
    }
}
