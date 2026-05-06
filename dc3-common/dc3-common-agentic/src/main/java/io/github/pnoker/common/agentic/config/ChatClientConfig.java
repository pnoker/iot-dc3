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

import io.github.pnoker.common.agentic.skill.SkillRegistry;
import io.github.pnoker.common.agentic.tool.AuthToolSet;
import io.github.pnoker.common.agentic.tool.DataToolSet;
import io.github.pnoker.common.agentic.tool.ManagerToolSet;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Spring AI ChatClient with tool registration and chat memory.
 */
@Configuration
@EnableConfigurationProperties(AgenticProperties.class)
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
            You are an intelligent assistant for the IoT DC3 platform.
            
            You can help users manage IoT devices, query real-time and historical data,
            and perform device operations. You have access to the following capabilities:
            
            - **Auth tools**: Look up tenants, users, and login records.
            - **Manager tools**: Query devices, drivers, and data points (metrics).
            - **Data tools**: Read real-time point values, query historical data, and send read/write commands to devices.
            
            Guidelines:
            - Always confirm before sending write commands to physical devices.
            - Present data in a clear, structured format.
            - If a query fails, explain the error and suggest alternatives.
            - Use the tools to fetch real data rather than making up values.
            """;

    @Bean
    @ConditionalOnMissingBean
    public ChatMemory agenticChatMemory(ChatMemoryRepository chatMemoryRepository,
                                        AgenticProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(properties.getMemoryMaxMessages())
                .build();
    }

    @Bean
    public ChatClient agenticChatClient(ChatClient.Builder builder,
                                        AuthToolSet authToolSet,
                                        ManagerToolSet managerToolSet,
                                        DataToolSet dataToolSet,
                                        ChatMemory agenticChatMemory,
                                        SkillRegistry skillRegistry) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(authToolSet, managerToolSet, dataToolSet)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(agenticChatMemory).build()
                )
                .build();
    }
}
