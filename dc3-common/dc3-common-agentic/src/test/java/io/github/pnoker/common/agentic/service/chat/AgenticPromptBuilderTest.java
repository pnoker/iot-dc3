/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.pnoker.common.agentic.service.chat;

import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the boundary between prepared agentic requests and Spring AI prompt wiring.
 */
@ExtendWith(MockitoExtension.class)
class AgenticPromptBuilderTest {

    @Mock
    private ChatClientFactory chatClientFactory;

    @Mock
    private ToolCallbackProvider toolCallbackProvider;

    @Mock
    private Advisor toolCallAdvisor;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec promptSpec;

    private AgenticPromptBuilder promptBuilder;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        promptBuilder = new AgenticPromptBuilder(chatClientFactory, toolCallbackProvider, toolCallAdvisor);
        when(chatClientFactory.getOrCreate("dc3-test-model")).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.tools(any(Consumer.class))).thenReturn(promptSpec);
        when(promptSpec.advisors(any(Consumer.class))).thenReturn(promptSpec);
        when(promptSpec.system(anyString())).thenReturn(promptSpec);
    }

    @Test
    void buildAttachesToolCallbacksAndExplicitToolCallAdvisorWhenToolCallingIsEnabled() {
        when(promptSpec.advisors(toolCallAdvisor)).thenReturn(promptSpec);

        promptBuilder.build(prepared(true));

        verify(promptSpec).advisors(toolCallAdvisor);
    }

    @Test
    void buildDoesNotAttachToolLoopWhenToolCallingIsDisabled() {
        promptBuilder.build(prepared(false));

        verify(promptSpec, never()).advisors(toolCallAdvisor);
    }

    @Test
    void buildDoesNotAdvertisePlatformToolsWhenToolCallingIsDisabled() {
        promptBuilder.build(prepared(false));

        var systemPrompt = forClass(String.class);
        verify(promptSpec).system(systemPrompt.capture());
        assertThat(systemPrompt.getValue()).doesNotContain("native tool calls");
        assertThat(systemPrompt.getValue()).doesNotContain("Device, driver, profile, and point metadata lookup");
    }

    @Test
    void buildAdvertisesPlatformToolsWhenToolCallingIsEnabled() {
        when(promptSpec.advisors(toolCallAdvisor)).thenReturn(promptSpec);

        promptBuilder.build(prepared(true));

        var systemPrompt = forClass(String.class);
        verify(promptSpec).system(systemPrompt.capture());
        assertThat(systemPrompt.getValue()).contains("native tool calls");
        assertThat(systemPrompt.getValue()).contains("Device, driver, profile, and point metadata lookup");
    }

    private AgenticPreparedChatRequest prepared(boolean toolCallingEnabled) {
        return new AgenticPreparedChatRequest("show me device status", "tenant:user:conversation",
                null, "dc3-test-model", Map.of(), null, null, new AgenticRunTrace(), toolCallingEnabled,
                false, List.of(), List.of(), AgenticMessageContent.Tokens.of(1, 0, 1, 0, 0, 0), List.of());
    }

}
