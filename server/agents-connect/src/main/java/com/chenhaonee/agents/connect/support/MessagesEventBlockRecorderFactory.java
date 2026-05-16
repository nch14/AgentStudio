package com.chenhaonee.agents.connect.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import com.chenhaonee.agents.domain.session.model.ContentBlockType;
import com.chenhaonee.agents.domain.session.model.MessageProtocolType;
import com.chenhaonee.agents.domain.session.model.MessageRole;
import com.chenhaonee.agents.domain.session.service.AgentSessionDomainService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Provider 无关的 MessagesEvent 流 → AgentMessage block 落库 recorder 工厂。
 *
 * <p>消费 Anthropic SSE 风格事件（含本项目自定义的 tool_result / turn_start / turn_stop /
 * interrupted 伪事件），按 block 边界实时落库。protocolType 由调用方注入，仅作为
 * agent_message 元数据写入，不参与任何分支。</p>
 */
@Component
public class MessagesEventBlockRecorderFactory {

    private final AgentSessionDomainService agentSessionDomainService;

    public MessagesEventBlockRecorderFactory(AgentSessionDomainService agentSessionDomainService) {
        this.agentSessionDomainService = agentSessionDomainService;
    }

    public MessagesEventBlockRecorder create(String sessionCode, String turnCode, MessageProtocolType protocolType) {
        return create(sessionCode, turnCode, protocolType, null);
    }

    public MessagesEventBlockRecorder create(
            String sessionCode,
            String turnCode,
            MessageProtocolType protocolType,
            String phase
    ) {
        return new MessagesEventBlockRecorder(sessionCode, turnCode, protocolType, phase);
    }

    /**
     * 单次会话内的事件累积器，每完成一个 block 即落库一条 AgentMessage。
     */
    public class MessagesEventBlockRecorder {

        private final String sessionCode;
        private final String turnCode;
        private final MessageProtocolType protocolType;
        private final String phase;
        private final Map<Integer, BlockBuffer> blockBuffers = new HashMap<>();
        private String currentExternalMessageId;
        private boolean turnStarted = false;

        private MessagesEventBlockRecorder(
                String sessionCode,
                String turnCode,
                MessageProtocolType protocolType,
                String phase
        ) {
            this.sessionCode = sessionCode;
            this.turnCode = turnCode;
            this.protocolType = protocolType;
            this.phase = trimToNull(phase);
        }

        public void onEvent(MessagesEvent event) {
            if (event == null || event.dataJson() == null || event.dataJson().isBlank()) {
                return;
            }
            JSONObject data = JSON.parseObject(event.dataJson());
            if (data == null) {
                return;
            }
            switch (event.eventType()) {
                case "message_start" -> onMessageStart(data);
                case "content_block_start" -> onContentBlockStart(data);
                case "content_block_delta" -> onContentBlockDelta(data);
                case "content_block_stop" -> onContentBlockStop(data);
                case "tool_result" -> persistToolResult(data);
                case "turn_start" -> persistTurnStart();
                case "turn_stop" -> persistTurnStop();
                case "interrupted" -> onInterrupted();
                default -> {
                }
            }
        }

        private void onMessageStart(JSONObject data) {
            JSONObject message = data.getJSONObject("message");
            if (message == null) {
                return;
            }
            currentExternalMessageId = message.getString("id");
            blockBuffers.clear();
        }

        private void onContentBlockStart(JSONObject data) {
            Integer index = data.getInteger("index");
            JSONObject contentBlock = data.getJSONObject("content_block");
            if (index == null || contentBlock == null) {
                return;
            }
            BlockBuffer buffer = new BlockBuffer();
            buffer.type = contentBlock.getString("type");
            buffer.text.append(safeString(contentBlock.getString("text")));
            buffer.thinking.append(safeString(contentBlock.getString("thinking")));
            buffer.signature.append(safeString(contentBlock.getString("signature")));
            buffer.toolUseId = contentBlock.getString("id");
            buffer.toolName = contentBlock.getString("name");
            buffer.initialInput = contentBlock.get("input");
            blockBuffers.put(index, buffer);
        }

        private void onContentBlockDelta(JSONObject data) {
            Integer index = data.getInteger("index");
            JSONObject delta = data.getJSONObject("delta");
            if (index == null || delta == null) {
                return;
            }
            BlockBuffer buffer = blockBuffers.get(index);
            if (buffer == null) {
                return;
            }
            switch (delta.getString("type")) {
                case "text_delta" -> buffer.text.append(safeString(delta.getString("text")));
                case "thinking_delta" -> buffer.thinking.append(safeString(delta.getString("thinking")));
                case "signature_delta" -> buffer.signature.append(safeString(delta.getString("signature")));
                case "input_json_delta" -> buffer.toolInput.append(safeString(delta.getString("partial_json")));
                default -> {
                }
            }
        }

        private void onContentBlockStop(JSONObject data) {
            Integer index = data.getInteger("index");
            if (index == null) {
                return;
            }
            BlockBuffer buffer = blockBuffers.remove(index);
            if (buffer == null || buffer.type == null) {
                return;
            }
            switch (buffer.type) {
                case "text" -> persistAssistantBlock(
                        ContentBlockType.TEXT,
                        AgentMessageBlockPayloads.text(buffer.text.toString())
                );
                case "thinking" -> persistAssistantBlock(
                        ContentBlockType.THINKING,
                        AgentMessageBlockPayloads.thinking(buffer.thinking.toString(), emptyToNull(buffer.signature.toString()))
                );
                case "tool_use" -> persistAssistantBlock(
                        ContentBlockType.TOOL_USE,
                        AgentMessageBlockPayloads.toolUse(
                                buffer.toolUseId,
                                buffer.toolName,
                                resolveToolInput(buffer)
                        )
                );
                default -> {
                }
            }
        }

        private void persistToolResult(JSONObject data) {
            agentSessionDomainService.appendBlock(
                    sessionCode,
                    turnCode,
                    MessageRole.TOOL,
                    ContentBlockType.TOOL_RESULT,
                    protocolType,
                    decoratePhase(JSON.toJSONString(data)),
                    null
            );
        }

        private void persistTurnStart() {
            turnStarted = true;
            agentSessionDomainService.appendBlock(
                    sessionCode,
                    turnCode,
                    MessageRole.ASSISTANT,
                    ContentBlockType.TURN_START,
                    protocolType,
                    "{}",
                    null
            );
        }

        private void onInterrupted() {
            // 把进行中的 text/thinking buffer 以 CANCELLED 状态落库，再补 TURN_STOP
            for (BlockBuffer buffer : blockBuffers.values()) {
                if (buffer.type == null) {
                    continue;
                }
                switch (buffer.type) {
                    case "text" -> {
                        String text = buffer.text.toString();
                        if (!text.isEmpty()) {
                            agentSessionDomainService.appendCancelledBlock(
                                    sessionCode, turnCode, MessageRole.ASSISTANT, ContentBlockType.TEXT,
                                    protocolType, decoratePhase(AgentMessageBlockPayloads.text(text)), currentExternalMessageId
                            );
                        }
                    }
                    case "thinking" -> {
                        String thinking = buffer.thinking.toString();
                        if (!thinking.isEmpty()) {
                            agentSessionDomainService.appendCancelledBlock(
                                    sessionCode, turnCode, MessageRole.ASSISTANT, ContentBlockType.THINKING,
                                    protocolType, decoratePhase(AgentMessageBlockPayloads.thinking(thinking, emptyToNull(buffer.signature.toString()))), currentExternalMessageId
                            );
                        }
                    }
                    default -> {
                    }
                }
            }
            blockBuffers.clear();
            if (turnStarted) {
                persistTurnStop();
            }
        }

        private void persistTurnStop() {
            // reset turnStarted 防止迟到的 interrupted 再补一次 TURN_STOP
            turnStarted = false;
            agentSessionDomainService.appendBlock(
                    sessionCode,
                    turnCode,
                    MessageRole.ASSISTANT,
                    ContentBlockType.TURN_STOP,
                    protocolType,
                    "{}",
                    null
            );
        }

        private void persistAssistantBlock(ContentBlockType blockType, String payloadJson) {
            agentSessionDomainService.appendBlock(
                    sessionCode,
                    turnCode,
                    MessageRole.ASSISTANT,
                    blockType,
                    protocolType,
                    decoratePhase(payloadJson),
                    currentExternalMessageId
            );
        }

        private String decoratePhase(String payloadJson) {
            if (phase == null) {
                return payloadJson;
            }
            JSONObject payload = JSON.parseObject(payloadJson);
            payload.put("source", "phase");
            payload.put("phase", phase);
            return JSON.toJSONString(payload);
        }

        private Object resolveToolInput(BlockBuffer buffer) {
            if (!buffer.toolInput.isEmpty()) {
                return AgentMessageBlockPayloads.parseJsonObjectOrRaw(buffer.toolInput.toString());
            }
            return buffer.initialInput == null ? new JSONObject() : buffer.initialInput;
        }

        private String safeString(String value) {
            return value == null ? "" : value;
        }

        private String emptyToNull(String value) {
            return value == null || value.isBlank() ? null : value;
        }

        private String trimToNull(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return value.trim();
        }
    }

    private static final class BlockBuffer {
        private String type;
        private String toolUseId;
        private String toolName;
        private Object initialInput;
        private final StringBuilder text = new StringBuilder();
        private final StringBuilder thinking = new StringBuilder();
        private final StringBuilder signature = new StringBuilder();
        private final StringBuilder toolInput = new StringBuilder();
    }
}
