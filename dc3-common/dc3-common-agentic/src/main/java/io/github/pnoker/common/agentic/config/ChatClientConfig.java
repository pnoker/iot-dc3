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
package io.github.pnoker.common.agentic.config;

import io.github.pnoker.common.agentic.service.MessageService;
import io.github.pnoker.common.agentic.service.runtime.AgenticToolTracingCallbackProvider;
import io.github.pnoker.common.agentic.tools.DeviceTool;
import io.github.pnoker.common.agentic.tools.DriverTool;
import io.github.pnoker.common.agentic.tools.PointTool;
import io.github.pnoker.common.agentic.tools.PointValueTool;
import io.github.pnoker.common.agentic.tools.ProfileTool;
import io.github.pnoker.common.agentic.tools.SystemTool;
import io.github.pnoker.common.agentic.tools.TenantTool;
import io.github.pnoker.common.agentic.tools.UserTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.ObjectMapper;

/**
 * Configures Spring AI chat memory backed by the {@code dc3_message} business table
 * via {@link MessageChatMemoryRepository}. The {@link ChatClient} instances are
 * created dynamically per provider by {@link ChatClientFactory} and wired with the
 * {@link MessageChatMemoryAdvisor} bean defined here.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2016.10.1
 */
@Configuration
@EnableConfigurationProperties(AgenticProperties.class)
public class ChatClientConfig {

    public static final String BASE_SYSTEM_PROMPT = """
            You are the IoT DC3 platform assistant.
            """;

    public static final String TOOL_SYSTEM_PROMPT = """
            Platform data access and platform actions are available through native tool calls attached to this request.
            
            - Tenant and user context lookup.
            - Device, driver, profile, and point metadata lookup.
            - Point-value read, history, read-command, and pending write action preparation.
            - System health lookup.
            """;

    @Bean
    @Primary
    public ChatMemoryRepository agenticChatMemoryRepository(MessageService messageService,
                                                            AgenticProperties properties) {
        return new MessageChatMemoryRepository(messageService, properties);
    }

    @Bean
    @Primary
    public ChatMemory agenticChatMemory(@Qualifier("agenticChatMemoryRepository") ChatMemoryRepository chatMemoryRepository,
                                        AgenticProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(properties.getMemoryMaxMessages())
                .build();
    }

    @Bean
    public Advisor agenticChatMemoryAdvisor(@Qualifier("agenticChatMemory") ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    public Advisor agenticToolCallAdvisor(ToolCallingManager toolCallingManager) {
        return ToolCallAdvisor.builder()
                .toolCallingManager(toolCallingManager)
                .advisorOrder(Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 100)
                .streamToolCallResponses(false)
                .build();
    }

    @Bean
    public ToolCallbackProvider agenticToolCallbackProvider(TenantTool tenantTool, UserTool userTool,
                                                            DeviceTool deviceTool, DriverTool driverTool,
                                                            ProfileTool profileTool, PointTool pointTool,
                                                            PointValueTool pointValueTool, SystemTool systemTool,
                                                            ObjectMapper objectMapper) {
        ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(tenantTool, userTool, deviceTool, driverTool, profileTool, pointTool, pointValueTool,
                        systemTool)
                .build();
        return new AgenticToolTracingCallbackProvider(provider, objectMapper, tenantTool, userTool, deviceTool,
                driverTool, profileTool, pointTool, pointValueTool, systemTool);
    }

    @Bean
    @Primary
    public ChatClient.Builder agenticChatClientBuilder(@Qualifier("openAiChatModel") ChatModel chatModel,
                                                       @Qualifier("agenticChatMemoryAdvisor") Advisor memoryAdvisor) {
        return ChatClient.builder(chatModel).defaultAdvisors(memoryAdvisor);
    }

}
