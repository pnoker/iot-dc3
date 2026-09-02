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

import static io.github.pnoker.common.utils.LogSanitizer.sanitize;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.repository.ReactiveModelConfigStore;
import io.github.pnoker.common.agentic.repository.ReactiveModelProviderStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Resolves tenant-scoped model configuration reactively and caches transport clients.
 * Database access is completed before prompt construction; synchronous cache reads never
 * cross the persistence boundary.
 */
@Slf4j
@Component
public class ChatClientFactory {

    private record ConfigKey(Long tenantId, String model) {}

    private record ProviderKey(Long tenantId, Long providerId) {}

    private final Map<ProviderKey, ChatClient> clientCache = new ConcurrentHashMap<>();
    private final Map<ProviderKey, ModelProviderBO> providerCache = new ConcurrentHashMap<>();
    private final Map<ConfigKey, ModelConfigBO> configCache = new ConcurrentHashMap<>();
    private final Map<Long, ModelConfigBO> defaultConfigCache = new ConcurrentHashMap<>();
    private final ReactiveModelProviderStore modelProviderStore;
    private final ReactiveModelConfigStore modelConfigStore;
    private final ChatClient.Builder fallbackBuilder;
    private final AgenticProperties properties;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String fallbackModel;

    public ChatClientFactory(
            ReactiveModelProviderStore modelProviderStore,
            ReactiveModelConfigStore modelConfigStore,
            ChatClient.Builder fallbackBuilder,
            AgenticProperties properties) {
        this.modelProviderStore = modelProviderStore;
        this.modelConfigStore = modelConfigStore;
        this.fallbackBuilder = fallbackBuilder;
        this.properties = properties;
    }

    public Mono<String> resolveModelReactive(String requestedModel, RequestHeader.PrincipalHeader header) {
        String candidate = StringUtils.trimToNull(requestedModel);
        Mono<ModelConfigBO> selected = candidate == null ? Mono.empty() : loadConfig(candidate, header);
        return selected.switchIfEmpty(loadDefaultConfig(header))
                .flatMap(config -> warmProvider(config, header).thenReturn(config.getModel()))
                .switchIfEmpty(Mono.fromSupplier(() -> fallbackModel(candidate)));
    }

    public Mono<Boolean> supportsToolCallReactive(String model, RequestHeader.PrincipalHeader header) {
        String candidate = StringUtils.trimToNull(model);
        Mono<ModelConfigBO> selected = candidate == null ? Mono.empty() : loadConfig(candidate, header);
        return selected.switchIfEmpty(loadDefaultConfig(header))
                .map(config -> Boolean.TRUE.equals(config.getToolCall()))
                .defaultIfEmpty(StringUtils.isNotBlank(fallbackModel)
                        && Strings.CS.equals(candidate, fallbackModel)
                        && properties.isFallbackToolCallingEnabled());
    }

    public ChatClient getOrCreate(String model, Long tenantId) {
        ModelConfigBO config = configCache.get(new ConfigKey(tenantId, model));
        if (config == null) config = defaultConfigCache.get(tenantId);
        if (config == null) {
            log.debug(
                    "Agentic model config cache miss, using fallback ChatClient, tenantId={}, model={}",
                    tenantId,
                    sanitize(model));
            return fallbackBuilder.build();
        }
        return getOrCreateByProvider(tenantId, config.getProviderId());
    }

    public ModelProviderBO resolveProviderForModel(String model, Long tenantId) {
        ModelConfigBO config = configCache.get(new ConfigKey(tenantId, model));
        if (config == null) config = defaultConfigCache.get(tenantId);
        return config == null ? null : providerCache.get(new ProviderKey(tenantId, config.getProviderId()));
    }

    public AgenticModelProviderTypeEnum resolveProviderType(String model, Long tenantId) {
        ModelProviderBO provider = resolveProviderForModel(model, tenantId);
        return provider == null ? null : provider.getProviderType();
    }

    public boolean supportsToolCall(String model, Long tenantId) {
        ModelConfigBO config = configCache.get(new ConfigKey(tenantId, model));
        if (config == null) config = defaultConfigCache.get(tenantId);
        if (config != null) return Boolean.TRUE.equals(config.getToolCall());
        return StringUtils.isNotBlank(fallbackModel)
                && Strings.CS.equals(model, fallbackModel)
                && properties.isFallbackToolCallingEnabled();
    }

    public void evict(Long providerId) {
        providerCache.keySet().removeIf(key -> Objects.equals(key.providerId(), providerId));
        clientCache.entrySet().removeIf(entry -> {
            boolean remove = Objects.equals(entry.getKey().providerId(), providerId);
            if (remove) log.info("Agentic ChatClient cache evicted, providerId={}", providerId);
            return remove;
        });
        configCache.entrySet().removeIf(entry -> Objects.equals(entry.getValue().getProviderId(), providerId));
        defaultConfigCache
                .entrySet()
                .removeIf(entry -> Objects.equals(entry.getValue().getProviderId(), providerId));
    }

    public ChatOptions.Builder<?> buildChatOptionsBuilder(
            String model, Long tenantId, Double temperature, Integer maxTokens) {
        if (StringUtils.isBlank(model) && temperature == null && maxTokens == null) return null;
        AgenticModelProviderTypeEnum providerType = resolveProviderType(model, tenantId);
        if (AgenticModelProviderTypeEnum.ANTHROPIC.equals(providerType)) {
            return applyCommonOptions(AnthropicChatOptions.builder(), model, temperature, maxTokens);
        }
        return applyCommonOptions(OpenAiChatOptions.builder(), model, temperature, maxTokens);
    }

    private Mono<ModelConfigBO> loadConfig(String model, RequestHeader.PrincipalHeader header) {
        ConfigKey key = new ConfigKey(header.getTenantId(), model);
        ModelConfigBO cached = configCache.get(key);
        if (cached != null) return Mono.just(cached);
        return modelConfigStore.findByModel(model, header).doOnNext(this::cacheConfig);
    }

    private Mono<ModelConfigBO> loadDefaultConfig(RequestHeader.PrincipalHeader header) {
        ModelConfigBO cached = defaultConfigCache.get(header.getTenantId());
        if (cached != null) return Mono.just(cached);
        return modelConfigStore.findDefault(header).doOnNext(this::cacheConfig);
    }

    private Mono<ModelProviderBO> warmProvider(ModelConfigBO config, RequestHeader.PrincipalHeader header) {
        ProviderKey key = new ProviderKey(header.getTenantId(), config.getProviderId());
        ModelProviderBO cached = providerCache.get(key);
        if (cached != null) return Mono.just(cached);
        return modelProviderStore
                .get(config.getProviderId(), header)
                .doOnNext(provider -> providerCache.put(key, provider));
    }

    private void cacheConfig(ModelConfigBO config) {
        configCache.put(new ConfigKey(config.getTenantId(), config.getModel()), config);
        if (config.getDefaultFlag() != null && config.getDefaultFlag().getIndex() == 1) {
            defaultConfigCache.put(config.getTenantId(), config);
        }
    }

    private String fallbackModel(String candidate) {
        String fallback = StringUtils.trimToNull(fallbackModel);
        if (StringUtils.isNotBlank(fallback)) {
            if (StringUtils.isNotBlank(candidate) && !Strings.CS.equals(candidate, fallback)) {
                log.warn(
                        "Agentic requested model is not configured, falling back to Spring AI model, requestedModel={}, fallbackModel={}",
                        sanitize(candidate),
                        sanitize(fallback));
            }
            return fallback;
        }
        return candidate;
    }

    private ChatClient getOrCreateByProvider(Long tenantId, Long providerId) {
        ProviderKey key = new ProviderKey(tenantId, providerId);
        return clientCache.computeIfAbsent(key, ignored -> {
            ModelProviderBO provider = providerCache.get(key);
            if (provider == null
                    || provider.getEnableFlag() == null
                    || provider.getEnableFlag().getIndex() != 0) {
                log.warn("Agentic provider cache miss or disabled, tenantId={}, providerId={}", tenantId, providerId);
                return fallbackBuilder.build();
            }
            ChatClient client = AgenticModelProviderTypeEnum.ANTHROPIC.equals(provider.getProviderType())
                    ? buildAnthropicClient(provider)
                    : buildOpenAiClient(provider);
            log.info(
                    "Agentic ChatClient created, tenantId={}, providerId={}, name={}, type={}, baseUrl={}",
                    tenantId,
                    providerId,
                    provider.getName(),
                    provider.getProviderType(),
                    provider.getBaseUrl());
            return client;
        });
    }

    private <B extends ChatOptions.Builder<B>> B applyCommonOptions(
            B builder, String model, Double temperature, Integer maxTokens) {
        if (StringUtils.isNotBlank(model)) builder.model(model);
        if (temperature != null) builder.temperature(temperature);
        if (maxTokens != null) builder.maxTokens(maxTokens);
        return builder;
    }

    private ChatClient buildOpenAiClient(ModelProviderBO provider) {
        OpenAIClient syncClient = OpenAIOkHttpClient.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        OpenAIClientAsync asyncClient = OpenAIOkHttpClientAsync.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiClient(syncClient)
                .openAiClientAsync(asyncClient)
                .build();
        return ChatClient.builder(model).build();
    }

    private ChatClient buildAnthropicClient(ModelProviderBO provider) {
        AnthropicClient syncClient = AnthropicOkHttpClient.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        AnthropicClientAsync asyncClient = AnthropicOkHttpClientAsync.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        AnthropicChatModel model = AnthropicChatModel.builder()
                .anthropicClient(syncClient)
                .anthropicClientAsync(asyncClient)
                .build();
        return ChatClient.builder(model).build();
    }
}
