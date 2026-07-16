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
import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import io.github.pnoker.common.agentic.dal.ModelConfigManager;
import io.github.pnoker.common.agentic.dal.ModelProviderManager;
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.builder.ModelConfigBuilder;
import io.github.pnoker.common.agentic.entity.builder.ModelProviderBuilder;
import io.github.pnoker.common.agentic.entity.model.ModelConfigDO;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.extern.slf4j.Slf4j;
import static io.github.pnoker.common.utils.LogSanitizer.sanitize;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and caches {@link ChatClient} instances per provider.
 * Falls back to the Spring AI auto-configured {@link ChatClient.Builder} when no DB provider matches.
 * Every {@link ChatClient} produced here is wired with the agentic
 * {@link Advisor memory advisor} so that conversation history persisted in
 * {@code dc3_message} is replayed back to the model on each call.
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
    private final Advisor memoryAdvisor;
    private final AgenticProperties properties;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String fallbackModel;

    public ChatClientFactory(ModelProviderManager modelProviderManager,
                             ModelConfigManager modelConfigManager,
                             ModelProviderBuilder modelProviderBuilder,
                             ModelConfigBuilder modelConfigBuilder,
                             ChatClient.Builder fallbackBuilder,
                             @Qualifier("agenticChatMemoryAdvisor") Advisor memoryAdvisor,
                             AgenticProperties properties) {
        this.modelProviderManager = modelProviderManager;
        this.modelConfigManager = modelConfigManager;
        this.modelProviderBuilder = modelProviderBuilder;
        this.modelConfigBuilder = modelConfigBuilder;
        this.fallbackBuilder = fallbackBuilder;
        this.memoryAdvisor = memoryAdvisor;
        this.properties = properties;
    }

    /**
     * Resolve the model identifier with a three-level fallback: the requested model if
     * configured, then the default model, then the Spring AI fallback model.
     *
     * @param requestedModel the model identifier from the request, may be null/blank
     * @return the resolved model identifier
     */
    public String resolveModel(String requestedModel) {
        String candidate = StringUtils.trimToNull(requestedModel);
        if (StringUtils.isNotBlank(candidate)) {
            ModelConfigBO requestedConfig = resolveConfig(candidate);
            if (Objects.nonNull(requestedConfig) && StringUtils.isNotBlank(requestedConfig.getModel())) {
                return requestedConfig.getModel();
            }
        }
        ModelConfigBO defaultConfig = resolveDefaultConfig();
        if (Objects.nonNull(defaultConfig) && StringUtils.isNotBlank(defaultConfig.getModel())) {
            if (StringUtils.isNotBlank(candidate)) {
                log.warn("Agentic requested model is not configured, falling back to default model, requestedModel={}, defaultModel={}",
                        sanitize(candidate), defaultConfig.getModel());
            }
            return defaultConfig.getModel();
        }
        String fallback = StringUtils.trimToNull(fallbackModel);
        if (StringUtils.isNotBlank(fallback)) {
            if (StringUtils.isNotBlank(candidate) && !StringUtils.equals(candidate, fallback)) {
                log.warn("Agentic requested model is not configured, falling back to Spring AI model, requestedModel={}, fallbackModel={}",
                        candidate, fallback);
            }
            return fallback;
        }
        return candidate;
    }

    /**
     * Get or create a cached ChatClient for the given model, resolving its config and
     * provider. Falls back to the Spring AI ChatClient when no config is found.
     *
     * @param model the model identifier, may be null/blank to use the default config
     * @return a ChatClient for the model
     */
    public ChatClient getOrCreate(String model) {
        ModelConfigBO config = null;
        if (StringUtils.isNotBlank(model)) {
            config = resolveConfig(model);
        }
        if (Objects.isNull(config)) {
            config = resolveDefaultConfig();
        }
        if (Objects.isNull(config)) {
            log.debug("Agentic model config not found, using fallback ChatClient, model={}", sanitize(model));
            return fallbackBuilder.build();
        }
        return getOrCreateByProvider(config.getProviderId());
    }

    /**
     * Resolve the provider type for a given model identifier so callers can build
     * provider-aware {@link ChatOptions}.
     *
     * @param model model identifier (may be null/blank to use the default config)
     * @return resolved provider type, or {@code null} when no provider record is found
     */
    public AgenticModelProviderTypeEnum resolveProviderType(String model) {
        ModelProviderBO provider = resolveProviderForModel(model);
        return Objects.nonNull(provider) ? provider.getProviderType() : null;
    }

    /**
     * Resolve the provider backing a model, defaulting to the default model's provider
     * when the model is unknown.
     *
     * @param model the model identifier, may be null/blank to use the default config
     * @return the provider, or null when none is found
     */
    public ModelProviderBO resolveProviderForModel(String model) {
        ModelConfigBO config = StringUtils.isNotBlank(model) ? resolveConfig(model) : null;
        if (Objects.isNull(config)) {
            config = resolveDefaultConfig();
        }
        if (Objects.isNull(config)) {
            return null;
        }
        return resolveProvider(config.getProviderId());
    }

    /**
     * Return whether a model supports tool calling. Falls back to the Spring AI
     * fallback model's tool-calling flag when the model is unconfigured.
     *
     * @param model the model identifier, may be null/blank
     * @return true if the model supports tool calling
     */
    public boolean supportsToolCall(String model) {
        ModelConfigBO config = StringUtils.isNotBlank(model) ? resolveConfig(model) : null;
        if (Objects.isNull(config)) {
            config = resolveDefaultConfig();
        }
        if (Objects.nonNull(config)) {
            return Boolean.TRUE.equals(config.getToolCall());
        }
        return StringUtils.isNotBlank(fallbackModel) && StringUtils.equals(model, fallbackModel)
                && properties.isFallbackToolCallingEnabled();
    }

    public void evict(Long providerId) {
        ChatClient removed = cache.remove(providerId);
        if (Objects.nonNull(removed)) {
            log.info("Agentic ChatClient cache evicted, providerId={}", providerId);
        }
    }

    /**
     * Build a {@link ChatOptions.Builder} instance appropriate for the resolved
     * provider. Returns {@code null} when no override fields are supplied so callers
     * can skip options entirely and let the provider defaults apply.
     *
     * <p>Spring AI 2.0 M5 expects a builder rather than a fully built options
     * instance on {@code ChatClient.ChatClientRequestSpec.options(...)}; the
     * framework calls {@code build()} once it has merged the per-request and
     * default options.
     *
     * @param model       model name override
     * @param temperature sampling temperature override (0.0..2.0)
     * @param maxTokens   max output tokens override
     * @return options builder, or {@code null} when nothing needs overriding
     */
    public ChatOptions.Builder<?> buildChatOptionsBuilder(String model, Double temperature, Integer maxTokens) {
        if (StringUtils.isBlank(model) && Objects.isNull(temperature) && Objects.isNull(maxTokens)) {
            return null;
        }
        AgenticModelProviderTypeEnum providerType = resolveProviderType(model);
        if (AgenticModelProviderTypeEnum.ANTHROPIC.equals(providerType)) {
            return applyCommonOptions(AnthropicChatOptions.builder(), model, temperature, maxTokens);
        }
        // Default to OpenAI options for OpenAI itself and OpenAI-compatible providers
        // such as DeepSeek, Moonshot, Qwen — they all share the OpenAI request shape.
        return applyCommonOptions(OpenAiChatOptions.builder(), model, temperature, maxTokens);
    }

    private <B extends ChatOptions.Builder<B>> B applyCommonOptions(B builder, String model, Double temperature,
                                                                    Integer maxTokens) {
        if (StringUtils.isNotBlank(model)) {
            builder.model(model);
        }
        if (Objects.nonNull(temperature)) {
            builder.temperature(temperature);
        }
        if (Objects.nonNull(maxTokens)) {
            builder.maxTokens(maxTokens);
        }
        return builder;
    }

    private ModelConfigBO resolveConfig(String model) {
        ModelConfigDO entityDO = modelConfigManager.getOne(Wrappers.<ModelConfigDO>query()
                .lambda()
                .eq(ModelConfigDO::getModel, model)
                .eq(ModelConfigDO::getEnableFlag, EnableFlagEnum.ENABLE)
                .last(QueryWrapperConstant.LIMIT_ONE));
        return Objects.nonNull(entityDO) ? modelConfigBuilder.buildBOByDO(entityDO) : null;
    }

    private ModelConfigBO resolveDefaultConfig() {
        ModelConfigDO entityDO = modelConfigManager.getOne(Wrappers.<ModelConfigDO>query()
                .lambda()
                .eq(ModelConfigDO::getDefaultFlag, DefaultFlagEnum.DEFAULT)
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
        OpenAIClientAsync openAiClientAsync = OpenAIOkHttpClientAsync.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiClient(openAiClient)
                .openAiClientAsync(openAiClientAsync)
                .build();
        return ChatClient.builder(chatModel).defaultAdvisors(memoryAdvisor).build();
    }

    private ChatClient buildAnthropicClient(ModelProviderBO provider) {
        AnthropicClient anthropicClient = AnthropicOkHttpClient.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        AnthropicClientAsync anthropicClientAsync = AnthropicOkHttpClientAsync.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        AnthropicChatModel chatModel = AnthropicChatModel.builder()
                .anthropicClient(anthropicClient)
                .anthropicClientAsync(anthropicClientAsync)
                .build();
        return ChatClient.builder(chatModel).defaultAdvisors(memoryAdvisor).build();
    }

    private boolean isEnabled(EnableFlagEnum enableFlag) {
        return EnableFlagEnum.ENABLE.equals(enableFlag);
    }

}
