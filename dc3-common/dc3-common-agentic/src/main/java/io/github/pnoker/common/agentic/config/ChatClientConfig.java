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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configures Spring AI chat memory backed by the {@code dc3_message} business table
 * via {@link MessageChatMemoryRepository}. The {@link ChatClient} instances are
 * created dynamically per provider by {@link ChatClientFactory} and wired with the
 * {@link MessageChatMemoryAdvisor} bean defined here.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2022.1.0
 */
@Configuration
@EnableConfigurationProperties(AgenticProperties.class)
public class ChatClientConfig {

    public static final String SYSTEM_PROMPT = """
            You are an intelligent assistant for the IoT DC3 platform.

            You can help users manage IoT devices, query real-time and historical data,
            and perform device operations. You have access to the following capabilities:

            - **Auth tools**: Read the current low-sensitivity tenant and user context.
            - **Manager tools**: Query devices, drivers, and data points (metrics).
            - **Data tools**: Read real-time point values, query historical data, and send read/write commands to devices.

            Guidelines:
            - Always confirm before sending write commands to physical devices.
            - Present data in a clear, structured format.
            - If a query fails, explain the error and suggest alternatives.
            - Use the tools to fetch real data rather than making up values.
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
    @Primary
    public ChatClient.Builder agenticChatClientBuilder(@Qualifier("openAiChatModel") ChatModel chatModel,
                                                       @Qualifier("agenticChatMemoryAdvisor") Advisor memoryAdvisor) {
        return ChatClient.builder(chatModel).defaultAdvisors(memoryAdvisor);
    }

}
