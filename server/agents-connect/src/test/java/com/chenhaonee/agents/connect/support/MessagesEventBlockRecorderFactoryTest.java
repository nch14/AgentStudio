package com.chenhaonee.agents.connect.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import com.chenhaonee.agents.domain.session.model.ContentBlockType;
import com.chenhaonee.agents.domain.session.model.MessageProtocolType;
import com.chenhaonee.agents.domain.session.model.MessageRole;
import com.chenhaonee.agents.domain.session.service.AgentSessionDomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class MessagesEventBlockRecorderFactoryTest {

    @Mock
    private AgentSessionDomainService agentSessionDomainService;

    @Test
    void shouldPersistThinkingToolUseAndToolResultBlocksInOrder() {
        MessagesEventBlockRecorderFactory factory = new MessagesEventBlockRecorderFactory(agentSessionDomainService);
        MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder recorder =
                factory.create("session-1", "turn-1", MessageProtocolType.ANTHROPIC_MESSAGES);

        recorder.onEvent(new MessagesEvent("message_start", """
                {"message": {"id": "msg_1"}}
                """));
        recorder.onEvent(new MessagesEvent("content_block_start", """
                {"index": 0, "content_block": {"type": "thinking", "thinking": ""}}
                """));
        recorder.onEvent(new MessagesEvent("content_block_delta", """
                {"index": 0, "delta": {"type": "thinking_delta", "thinking": "先思考一下"}}
                """));
        recorder.onEvent(new MessagesEvent("content_block_delta", """
                {"index": 0, "delta": {"type": "signature_delta", "signature": "sig-1"}}
                """));
        recorder.onEvent(new MessagesEvent("content_block_stop", """
                {"index": 0}
                """));
        recorder.onEvent(new MessagesEvent("content_block_start", """
                {"index": 1, "content_block": {"type": "tool_use", "id": "toolu_1", "name": "Read"}}
                """));
        recorder.onEvent(new MessagesEvent("content_block_delta", """
                {"index": 1, "delta": {"type": "input_json_delta", "partial_json": "{\\"path\\":\\"README.md\\"}"}}
                """));
        recorder.onEvent(new MessagesEvent("content_block_stop", """
                {"index": 1}
                """));
        recorder.onEvent(new MessagesEvent("tool_result", """
                {"toolUseId": "toolu_1", "content": "file content", "isError": false}
                """));

        InOrder inOrder = inOrder(agentSessionDomainService);
        ArgumentCaptor<String> thinkingPayload = ArgumentCaptor.forClass(String.class);
        inOrder.verify(agentSessionDomainService).appendBlock(
                eq("session-1"),
                eq("turn-1"),
                eq(MessageRole.ASSISTANT),
                eq(ContentBlockType.THINKING),
                eq(MessageProtocolType.ANTHROPIC_MESSAGES),
                thinkingPayload.capture(),
                eq("msg_1")
        );
        JSONObject thinking = JSON.parseObject(thinkingPayload.getValue());
        assertEquals("先思考一下", thinking.getString("thinking"));
        assertEquals("sig-1", thinking.getString("signature"));

        ArgumentCaptor<String> toolUsePayload = ArgumentCaptor.forClass(String.class);
        inOrder.verify(agentSessionDomainService).appendBlock(
                eq("session-1"),
                eq("turn-1"),
                eq(MessageRole.ASSISTANT),
                eq(ContentBlockType.TOOL_USE),
                eq(MessageProtocolType.ANTHROPIC_MESSAGES),
                toolUsePayload.capture(),
                eq("msg_1")
        );
        JSONObject toolUse = JSON.parseObject(toolUsePayload.getValue());
        assertEquals("toolu_1", toolUse.getString("toolUseId"));
        assertEquals("Read", toolUse.getString("name"));
        assertEquals("README.md", toolUse.getJSONObject("input").getString("path"));

        ArgumentCaptor<String> toolResultPayload = ArgumentCaptor.forClass(String.class);
        inOrder.verify(agentSessionDomainService).appendBlock(
                eq("session-1"),
                eq("turn-1"),
                eq(MessageRole.TOOL),
                eq(ContentBlockType.TOOL_RESULT),
                eq(MessageProtocolType.ANTHROPIC_MESSAGES),
                toolResultPayload.capture(),
                isNull()
        );
        JSONObject toolResult = JSON.parseObject(toolResultPayload.getValue());
        assertEquals("toolu_1", toolResult.getString("toolUseId"));
        assertEquals("file content", toolResult.getString("content"));
        assertFalse(toolResult.getBooleanValue("isError"));
        verifyNoMoreInteractions(agentSessionDomainService);
    }

    @Test
    void shouldPersistTurnStartBlockOnTurnStartEvent() {
        MessagesEventBlockRecorderFactory factory = new MessagesEventBlockRecorderFactory(agentSessionDomainService);
        MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder recorder =
                factory.create("session-2", "turn-2", MessageProtocolType.ANTHROPIC_MESSAGES);

        recorder.onEvent(new MessagesEvent("turn_start", "{}"));

        verify(agentSessionDomainService).appendBlock(
                eq("session-2"),
                eq("turn-2"),
                eq(MessageRole.ASSISTANT),
                eq(ContentBlockType.TURN_START),
                eq(MessageProtocolType.ANTHROPIC_MESSAGES),
                eq("{}"),
                isNull()
        );
        verifyNoMoreInteractions(agentSessionDomainService);
    }

    @Test
    void shouldPersistTurnStopBlockOnTurnStopEvent() {
        MessagesEventBlockRecorderFactory factory = new MessagesEventBlockRecorderFactory(agentSessionDomainService);
        MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder recorder =
                factory.create("session-2", "turn-2", MessageProtocolType.ANTHROPIC_MESSAGES);

        recorder.onEvent(new MessagesEvent("turn_stop", "{}"));

        verify(agentSessionDomainService).appendBlock(
                eq("session-2"),
                eq("turn-2"),
                eq(MessageRole.ASSISTANT),
                eq(ContentBlockType.TURN_STOP),
                eq(MessageProtocolType.ANTHROPIC_MESSAGES),
                eq("{}"),
                isNull()
        );
        verifyNoMoreInteractions(agentSessionDomainService);
    }

    @Test
    void shouldFlushPartialTextBlockAsCancelledAndWriteTurnStopOnInterrupted() {
        MessagesEventBlockRecorderFactory factory = new MessagesEventBlockRecorderFactory(agentSessionDomainService);
        MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder recorder =
                factory.create("session-3", "turn-3", MessageProtocolType.ANTHROPIC_MESSAGES);

        recorder.onEvent(new MessagesEvent("turn_start", "{}"));
        recorder.onEvent(new MessagesEvent("message_start", """
                {"message": {"id": "msg_x"}}
                """));
        recorder.onEvent(new MessagesEvent("content_block_start", """
                {"index": 0, "content_block": {"type": "text", "text": ""}}
                """));
        recorder.onEvent(new MessagesEvent("content_block_delta", """
                {"index": 0, "delta": {"type": "text_delta", "text": "部分生成的"}}
                """));
        recorder.onEvent(new MessagesEvent("interrupted", "{}"));

        InOrder inOrder = inOrder(agentSessionDomainService);
        inOrder.verify(agentSessionDomainService).appendBlock(
                eq("session-3"), eq("turn-3"), eq(MessageRole.ASSISTANT),
                eq(ContentBlockType.TURN_START), eq(MessageProtocolType.ANTHROPIC_MESSAGES),
                eq("{}"), isNull()
        );
        ArgumentCaptor<String> cancelledPayload = ArgumentCaptor.forClass(String.class);
        inOrder.verify(agentSessionDomainService).appendCancelledBlock(
                eq("session-3"), eq("turn-3"), eq(MessageRole.ASSISTANT),
                eq(ContentBlockType.TEXT), eq(MessageProtocolType.ANTHROPIC_MESSAGES),
                cancelledPayload.capture(), eq("msg_x")
        );
        assertEquals("部分生成的", JSON.parseObject(cancelledPayload.getValue()).getString("text"));
        inOrder.verify(agentSessionDomainService).appendBlock(
                eq("session-3"), eq("turn-3"), eq(MessageRole.ASSISTANT),
                eq(ContentBlockType.TURN_STOP), eq(MessageProtocolType.ANTHROPIC_MESSAGES),
                eq("{}"), isNull()
        );
        verifyNoMoreInteractions(agentSessionDomainService);
    }

    @Test
    void shouldWriteTurnStopButNoPartialBlockWhenInterruptedWithEmptyBuffer() {
        MessagesEventBlockRecorderFactory factory = new MessagesEventBlockRecorderFactory(agentSessionDomainService);
        MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder recorder =
                factory.create("session-4", "turn-4", MessageProtocolType.ANTHROPIC_MESSAGES);

        recorder.onEvent(new MessagesEvent("turn_start", "{}"));
        recorder.onEvent(new MessagesEvent("interrupted", "{}"));

        InOrder inOrder = inOrder(agentSessionDomainService);
        inOrder.verify(agentSessionDomainService).appendBlock(
                eq("session-4"), eq("turn-4"), eq(MessageRole.ASSISTANT),
                eq(ContentBlockType.TURN_START), any(), eq("{}"), isNull()
        );
        inOrder.verify(agentSessionDomainService).appendBlock(
                eq("session-4"), eq("turn-4"), eq(MessageRole.ASSISTANT),
                eq(ContentBlockType.TURN_STOP), any(), eq("{}"), isNull()
        );
        verify(agentSessionDomainService, never()).appendCancelledBlock(any(), any(), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(agentSessionDomainService);
    }

    @Test
    void shouldNotWriteTurnStopOnInterruptedIfTurnNeverStarted() {
        MessagesEventBlockRecorderFactory factory = new MessagesEventBlockRecorderFactory(agentSessionDomainService);
        MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder recorder =
                factory.create("session-5", "turn-5", MessageProtocolType.ANTHROPIC_MESSAGES);

        recorder.onEvent(new MessagesEvent("interrupted", "{}"));

        verify(agentSessionDomainService, never()).appendBlock(any(), any(), any(), any(), any(), any(), any());
        verify(agentSessionDomainService, never()).appendCancelledBlock(any(), any(), any(), any(), any(), any(), any());
    }
}
