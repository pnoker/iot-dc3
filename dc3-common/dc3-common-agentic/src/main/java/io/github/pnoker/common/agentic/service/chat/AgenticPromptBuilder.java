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

import io.github.pnoker.common.agentic.config.ChatClientConfig;
import io.github.pnoker.common.agentic.config.ChatClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds Spring AI chat prompts from prepared request state.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2016.10.1
 */
@Component
public class AgenticPromptBuilder {

    private final ChatClientFactory chatClientFactory;

    private final ToolCallbackProvider toolCallbackProvider;

    private final Advisor toolCallAdvisor;

    public AgenticPromptBuilder(ChatClientFactory chatClientFactory,
                                @Qualifier("agenticToolCallbackProvider") ToolCallbackProvider toolCallbackProvider,
                                @Qualifier("agenticToolCallAdvisor") Advisor toolCallAdvisor) {
        this.chatClientFactory = chatClientFactory;
        this.toolCallbackProvider = toolCallbackProvider;
        this.toolCallAdvisor = toolCallAdvisor;
    }

    public ChatClient.ChatClientRequestSpec build(AgenticPreparedChatRequest prepared) {
        ChatClient chatClient = chatClientFactory.getOrCreate(prepared.model());
        ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt()
                .user(prepared.userMessage())
                .tools(toolSpec -> toolSpec.context(prepared.toolContext()))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, prepared.scopedConversationId()));

        String systemPrompt = buildSystemPrompt(prepared);
        if (StringUtils.isNotBlank(systemPrompt)) {
            promptSpec = promptSpec.system(systemPrompt);
        }
        promptSpec = applyToolCallbacks(promptSpec, prepared);
        promptSpec = applyRequestOptions(promptSpec, prepared.model(), prepared.temperature(), prepared.maxTokens());
        return promptSpec;
    }

    private ChatClient.ChatClientRequestSpec applyToolCallbacks(ChatClient.ChatClientRequestSpec promptSpec,
                                                                AgenticPreparedChatRequest prepared) {
        if (!prepared.toolCallingEnabled()) {
            return promptSpec;
        }
        return promptSpec.tools(toolSpec -> toolSpec.callbacks(toolCallbackProvider)).advisors(toolCallAdvisor);
    }

    private ChatClient.ChatClientRequestSpec applyRequestOptions(ChatClient.ChatClientRequestSpec promptSpec,
                                                                 String model, Double temperature, Integer maxTokens) {
        ChatOptions.Builder<?> optionsBuilder = chatClientFactory.buildChatOptionsBuilder(model, temperature, maxTokens);
        return Objects.nonNull(optionsBuilder) ? promptSpec.options(optionsBuilder) : promptSpec;
    }

    public String buildSystemPrompt(AgenticPreparedChatRequest prepared) {
        List<String> sections = new ArrayList<>();
        sections.add(ChatClientConfig.BASE_SYSTEM_PROMPT);
        if (prepared.toolCallingEnabled()) {
            sections.add(ChatClientConfig.TOOL_SYSTEM_PROMPT);
        }
        if (StringUtils.isNotBlank(prepared.requestSystemContext())) {
            sections.add(prepared.requestSystemContext().trim());
        }
        return String.join("\n\n", sections);
    }

}
