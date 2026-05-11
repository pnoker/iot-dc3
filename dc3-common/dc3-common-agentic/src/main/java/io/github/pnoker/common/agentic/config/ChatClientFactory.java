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

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import io.github.pnoker.common.agentic.dal.ModelConfigManager;
import io.github.pnoker.common.agentic.dal.ModelProviderManager;
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.builder.ModelConfigBuilder;
import io.github.pnoker.common.agentic.entity.builder.ModelProviderBuilder;
import io.github.pnoker.common.agentic.entity.model.ModelConfigDO;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and caches {@link ChatClient} instances per provider.
 * Falls back to the Spring AI auto-configured {@link ChatClient.Builder} when no DB provider matches.
 *
 * @author pnoker
 * @version 2026.5.11
 * @since 2026.5.10
 */
@Slf4j
@Component
public class ChatClientFactory {

    private final Map<Long, ChatClient> cache = new ConcurrentHashMap<>();
    private final ModelProviderManager modelProviderManager;
    private final ModelConfigManager modelConfigManager;
    private final ModelProviderBuilder modelProviderBuilder;
    private final ModelConfigBuilder modelConfigBuilder;
    private final ChatClient.Builder fallbackBuilder;

    public ChatClientFactory(ModelProviderManager modelProviderManager,
                             ModelConfigManager modelConfigManager,
                             ModelProviderBuilder modelProviderBuilder,
                             ModelConfigBuilder modelConfigBuilder,
                             ChatClient.Builder fallbackBuilder) {
        this.modelProviderManager = modelProviderManager;
        this.modelConfigManager = modelConfigManager;
        this.modelProviderBuilder = modelProviderBuilder;
        this.modelConfigBuilder = modelConfigBuilder;
        this.fallbackBuilder = fallbackBuilder;
    }

    public ChatClient getOrCreate(String model) {
        ModelConfigBO config = resolveConfig(model);
        if (Objects.isNull(config)) {
            log.debug("Agentic model config not found, using fallback ChatClient, model={}", model);
            return fallbackBuilder.build();
        }
        return getOrCreateByProvider(config.getProviderId());
    }

    public void evict(Long providerId) {
        ChatClient removed = cache.remove(providerId);
        if (Objects.nonNull(removed)) {
            log.info("Agentic ChatClient cache evicted, providerId={}", providerId);
        }
    }

    private ModelConfigBO resolveConfig(String model) {
        ModelConfigDO entityDO = modelConfigManager.getOne(Wrappers.<ModelConfigDO>query()
                .lambda()
                .eq(ModelConfigDO::getModel, model)
                .eq(ModelConfigDO::getEnableFlag, EnableFlagEnum.ENABLE)
                .last(QueryWrapperConstant.LIMIT_ONE));
        return Objects.nonNull(entityDO) ? modelConfigBuilder.buildBOByDO(entityDO) : null;
    }

    private ChatClient getOrCreateByProvider(Long providerId) {
        return cache.computeIfAbsent(providerId, id -> {
            ModelProviderBO provider = resolveProvider(id);
            if (Objects.isNull(provider) || !isEnabled(provider.getEnableFlag())) {
                log.warn("Agentic provider not found or disabled, providerId={}", id);
                return fallbackBuilder.build();
            }
            ChatClient chatClient;
            if (AgenticModelProviderTypeEnum.ANTHROPIC.equals(provider.getProviderType())) {
                chatClient = buildAnthropicClient(provider);
            } else {
                chatClient = buildOpenAiClient(provider);
            }
            log.info("Agentic ChatClient created, providerId={}, name={}, type={}, baseUrl={}",
                    id, provider.getName(), provider.getProviderType(), provider.getBaseUrl());
            return chatClient;
        });
    }

    private ModelProviderBO resolveProvider(Long providerId) {
        return Optional.ofNullable(modelProviderManager.getById(providerId))
                .map(modelProviderBuilder::buildBOByDO)
                .orElse(null);
    }

    private ChatClient buildOpenAiClient(ModelProviderBO provider) {
        OpenAIClient openAiClient = OpenAIOkHttpClient.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiClient(openAiClient)
                .build();
        return ChatClient.builder(chatModel).build();
    }

    private ChatClient buildAnthropicClient(ModelProviderBO provider) {
        AnthropicClient anthropicClient = AnthropicOkHttpClient.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        AnthropicChatModel chatModel = AnthropicChatModel.builder()
                .anthropicClient(anthropicClient)
                .build();
        return ChatClient.builder(chatModel).build();
    }

    private boolean isEnabled(EnableFlagEnum enableFlag) {
        return EnableFlagEnum.ENABLE.equals(enableFlag);
    }

}
