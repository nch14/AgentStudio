package com.chenhaonee.agents.app.application.agent;

import com.chenhaonee.agents.connect.spi.core.MessagesAgent;
import com.chenhaonee.agents.connect.spi.core.TaskAgent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderCapabilityServiceTest {

    @Test
    void shouldExposeClaudeCodeMessagesAndTaskCapabilities() {
        MessagesAgent claudeMessagesAgent = mock(MessagesAgent.class);
        TaskAgent claudeTaskAgent = mock(TaskAgent.class);

        when(claudeMessagesAgent.supportedType()).thenReturn(AgentProvider.CLAUDE_CODE);
        when(claudeTaskAgent.supportedType()).thenReturn(AgentProvider.CLAUDE_CODE);

        ProviderCapabilityService service = new ProviderCapabilityService(
                List.of(claudeTaskAgent),
                List.of(claudeMessagesAgent)
        );

        assertFalse(service.getCapability(AgentProvider.CLAUDE_CODE).supportChat());
        assertTrue(service.getCapability(AgentProvider.CLAUDE_CODE).supportTask());
        assertTrue(service.supportsMessages(AgentProvider.CLAUDE_CODE));
    }
}
