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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Resolves tenant-scoped model configuration reactively and caches transport clients.
 * Database access is completed before prompt construction; synchronous cache reads never
 * cross the persistence boundary.
 *
 * <p>Every in-process cache entry expires after {@code dc3.agentic.transport-cache-ttl-seconds}
 * so out-of-band provider edits cannot keep serving stale transports forever. The
 * platform-wide fallback client is never cached: a cache miss either re-reads the
 * provider or, when the fallback is explicitly enabled, builds an uncached fallback
 * client.
 */
@Slf4j
@Component
public class ChatClientFactory {

    private record ConfigKey(Long tenantId, String model) {}

    private record ProviderKey(Long tenantId, Long providerId) {}

    /** In-flight slot lease over the semaphore the request was admitted to. */
    public record InFlightLease(Semaphore semaphore) {}

    private final Cache<ProviderKey, ChatClient> clientCache;
    private final Cache<ProviderKey, ModelProviderBO> providerCache;
    private final Cache<ConfigKey, ModelConfigBO> configCache;
    private final Cache<Long, ModelConfigBO> defaultConfigCache;
    private final Map<ProviderKey, Semaphore> inFlightSemaphores = new ConcurrentHashMap<>();
    private final Semaphore globalInFlightSemaphore;
    private final ExecutorService slotExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Scheduler slotScheduler = Schedulers.fromExecutorService(slotExecutor);
    private final ReactiveModelProviderStore modelProviderStore;
    private final ReactiveModelConfigStore modelConfigStore;
    private final ChatClient.Builder fallbackBuilder;
    private final AgenticProperties properties;
    private final MeterRegistry meterRegistry;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String fallbackModel;

    /** chat client factory. */
    public ChatClientFactory(
            ReactiveModelProviderStore modelProviderStore,
            ReactiveModelConfigStore modelConfigStore,
            ChatClient.Builder fallbackBuilder,
            AgenticProperties properties,
            MeterRegistry meterRegistry) {
        this.modelProviderStore = modelProviderStore;
        this.modelConfigStore = modelConfigStore;
        this.fallbackBuilder = fallbackBuilder;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        Duration ttl = Duration.ofSeconds(properties.getTransportCacheTtlSeconds());
        this.clientCache = Caffeine.newBuilder().expireAfterWrite(ttl).build();
        this.providerCache = Caffeine.newBuilder().expireAfterWrite(ttl).build();
        this.configCache = Caffeine.newBuilder().expireAfterWrite(ttl).build();
        this.defaultConfigCache = Caffeine.newBuilder().expireAfterWrite(ttl).build();
        this.globalInFlightSemaphore = new Semaphore(properties.getTransportGlobalMaxConcurrentRequests());
        Gauge.builder(
                        "agentic.transport.global.permits",
                        globalInFlightSemaphore,
                        semaphore -> semaphore.availablePermits())
                .register(meterRegistry);
    }

    /** Resolve the effective chat model for the request, falling back to the tenant default and the configured fallback model. */
    public Mono<String> resolveModelReactive(String requestedModel, RequestHeader.PrincipalHeader header) {
        String candidate = StringUtils.trimToNull(requestedModel);
        Mono<ModelConfigBO> selected = candidate == null ? Mono.empty() : loadConfig(candidate, header);
        return selected.switchIfEmpty(loadDefaultConfig(header))
                .flatMap(config -> warmProvider(config, header).thenReturn(config.getModel()))
                .switchIfEmpty(Mono.defer(() -> {
                    if (!properties.isTransportFallbackEnabled()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "No model configuration exists for this tenant; configure a model provider "
                                        + "or enable dc3.agentic.transport-fallback-enabled"));
                    }
                    return Mono.fromSupplier(() -> fallbackModel(candidate));
                }));
    }

    /** Report whether the resolved model enables tool calling. */
    public Mono<Boolean> supportsToolCallReactive(String model, RequestHeader.PrincipalHeader header) {
        String candidate = StringUtils.trimToNull(model);
        Mono<ModelConfigBO> selected = candidate == null ? Mono.empty() : loadConfig(candidate, header);
        return selected.switchIfEmpty(loadDefaultConfig(header))
                .map(config -> Boolean.TRUE.equals(config.getToolCall()))
                .defaultIfEmpty(StringUtils.isNotBlank(fallbackModel)
                        && Strings.CS.equals(candidate, fallbackModel)
                        && properties.isFallbackToolCallingEnabled());
    }

    /** Return the cached ChatClient for the model, or the fallback client when no config is cached. */
    public ChatClient getOrCreate(String model, Long tenantId) {
        ModelConfigBO config = configCache.getIfPresent(new ConfigKey(tenantId, model));
        if (config == null) config = defaultConfigCache.getIfPresent(tenantId);
        if (config == null) {
            return uncachedFallbackClient(tenantId, model, "model config cache miss");
        }
        return getOrCreateByProvider(tenantId, config.getProviderId());
    }

    /** Return the cached model provider backing the model, or null when unresolved. */
    public ModelProviderBO resolveProviderForModel(String model, Long tenantId) {
        ModelConfigBO config = configCache.getIfPresent(new ConfigKey(tenantId, model));
        if (config == null) config = defaultConfigCache.getIfPresent(tenantId);
        return config == null ? null : providerCache.getIfPresent(new ProviderKey(tenantId, config.getProviderId()));
    }

    /** Return the provider type backing the model, or null when unresolved. */
    public AgenticModelProviderTypeEnum resolveProviderType(String model, Long tenantId) {
        ModelProviderBO provider = resolveProviderForModel(model, tenantId);
        return provider == null ? null : provider.getProviderType();
    }

    /** Report whether the cached model config enables tool calling. */
    public boolean supportsToolCall(String model, Long tenantId) {
        ModelConfigBO config = configCache.getIfPresent(new ConfigKey(tenantId, model));
        if (config == null) config = defaultConfigCache.getIfPresent(tenantId);
        if (config != null) return Boolean.TRUE.equals(config.getToolCall());
        return StringUtils.isNotBlank(fallbackModel)
                && Strings.CS.equals(model, fallbackModel)
                && properties.isFallbackToolCallingEnabled();
    }

    /**
     * Drop cached configs, providers and clients owned by the provider. The in-flight
     * semaphores deliberately survive the eviction: their key space is bounded by the
     * (tenant, provider) rows the database can hold, so keeping them cannot leak, while
     * removing one that still has outstanding leases would let a freshly rebuilt
     * semaphore admit a full extra round of requests (transient over-admission) and
     * orphan the already-registered permit gauge — the meter registry keeps the old
     * gauge under the same tags and silently drops the re-registration.
     */
    public void evict(Long providerId) {
        providerCache.asMap().keySet().removeIf(key -> Objects.equals(key.providerId(), providerId));
        clientCache.asMap().entrySet().removeIf(entry -> {
            boolean remove = Objects.equals(entry.getKey().providerId(), providerId);
            if (remove) log.info("Agentic ChatClient cache evicted, providerId={}", providerId);
            return remove;
        });
        configCache
                .asMap()
                .entrySet()
                .removeIf(entry -> Objects.equals(entry.getValue().getProviderId(), providerId));
        defaultConfigCache
                .asMap()
                .entrySet()
                .removeIf(entry -> Objects.equals(entry.getValue().getProviderId(), providerId));
    }

    /**
     * Acquire one in-flight model-request slot for the resolved provider, parking on
     * a virtual-thread scheduler while all slots are taken. Requests whose model has
     * no cached provider config are admitted against the global semaphore instead of
     * running unthrottled. Waiting longer than
     * {@code dc3.agentic.transport-acquire-timeout-seconds} fails fast with an
     * UNAVAILABLE problem instead of queueing forever.
     *
     * <p>Cancellation can land after {@code tryAcquire} granted the permit but before
     * the lease is emitted downstream; the discarded lease is routed back here and
     * released so the slot never leaks (verified against reactor-core's
     * {@code MonoCallable}: a callable value produced into a cancelled subscription
     * goes through {@code Operators.onDiscard}).
     */
    public Mono<InFlightLease> acquireInFlightSlot(String model, Long tenantId) {
        return Mono.fromCallable(() -> {
                    Semaphore semaphore = resolveInFlightSemaphore(model, tenantId);
                    boolean acquired =
                            semaphore.tryAcquire(properties.getTransportAcquireTimeoutSeconds(), TimeUnit.SECONDS);
                    if (!acquired) {
                        meterRegistry
                                .counter(
                                        "agentic.transport.acquire.timeout",
                                        "scope",
                                        semaphore == globalInFlightSemaphore ? "global" : "provider")
                                .increment();
                        throw new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "Timed out waiting for a free model request slot; retry shortly");
                    }
                    return new InFlightLease(semaphore);
                })
                .doOnDiscard(InFlightLease.class, this::releaseInFlightSlot)
                .subscribeOn(slotScheduler);
    }

    /** Release a lease acquired through {@link #acquireInFlightSlot(String, Long)}; null-safe. */
    public void releaseInFlightSlot(InFlightLease lease) {
        if (lease != null && lease.semaphore() != null) {
            lease.semaphore().release();
        }
    }

    private Semaphore resolveInFlightSemaphore(String model, Long tenantId) {
        if (tenantId != null) {
            ModelConfigBO config = configCache.getIfPresent(new ConfigKey(tenantId, model));
            if (config == null) {
                config = defaultConfigCache.getIfPresent(tenantId);
            }
            if (config != null) {
                ProviderKey key = new ProviderKey(tenantId, config.getProviderId());
                Semaphore semaphore = inFlightSemaphores.get(key);
                if (semaphore == null) {
                    semaphore = inFlightSemaphores.computeIfAbsent(key, ignored -> {
                        Semaphore created = new Semaphore(properties.getTransportMaxConcurrentRequests());
                        Gauge.builder("agentic.transport.provider.permits", created, value -> value.availablePermits())
                                .tag("tenant", String.valueOf(key.tenantId()))
                                .tag("provider", String.valueOf(key.providerId()))
                                .register(meterRegistry);
                        return created;
                    });
                }
                return semaphore;
            }
        }
        return globalInFlightSemaphore;
    }

    /** Build a provider-specific chat options builder, or null when nothing is set. */
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
        ModelConfigBO cached = configCache.getIfPresent(key);
        if (cached != null) return Mono.just(cached);
        return modelConfigStore.getByModel(model, header).doOnNext(this::cacheConfig);
    }

    private Mono<ModelConfigBO> loadDefaultConfig(RequestHeader.PrincipalHeader header) {
        ModelConfigBO cached = defaultConfigCache.getIfPresent(header.getTenantId());
        if (cached != null) return Mono.just(cached);
        return modelConfigStore.getDefault(header).doOnNext(this::cacheConfig);
    }

    private Mono<ModelProviderBO> warmProvider(ModelConfigBO config, RequestHeader.PrincipalHeader header) {
        ProviderKey key = new ProviderKey(header.getTenantId(), config.getProviderId());
        ModelProviderBO cached = providerCache.getIfPresent(key);
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

    /**
     * Resolve the fallback path on a cache miss: the fallback client is built fresh
     * (never cached) and only when the deployment explicitly enabled it; otherwise
     * the request fails with a clear UNAVAILABLE problem instead of silently using
     * the platform-wide key.
     */
    private ChatClient uncachedFallbackClient(Long tenantId, String model, String reason) {
        if (!properties.isTransportFallbackEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No usable model configuration for this tenant (" + reason + "); configure a model provider "
                            + "or enable dc3.agentic.transport-fallback-enabled");
        }
        log.debug(
                "Agentic {} - serving an uncached fallback ChatClient, tenantId={}, model={}",
                reason,
                tenantId,
                sanitize(model));
        return fallbackBuilder.build();
    }

    private ChatClient getOrCreateByProvider(Long tenantId, Long providerId) {
        ProviderKey key = new ProviderKey(tenantId, providerId);
        ChatClient cached = clientCache.getIfPresent(key);
        if (cached != null) return cached;
        ModelProviderBO provider = providerCache.getIfPresent(key);
        if (provider == null
                || provider.getEnableFlag() == null
                || provider.getEnableFlag().getIndex() != 0) {
            log.warn("Agentic provider cache miss or disabled, tenantId={}, providerId={}", tenantId, providerId);
            return uncachedFallbackClient(tenantId, String.valueOf(providerId), "provider cache miss or disabled");
        }
        ChatClient client = AgenticModelProviderTypeEnum.ANTHROPIC.equals(provider.getProviderType())
                ? buildAnthropicClient(provider)
                : buildOpenAiClient(provider);
        clientCache.put(key, client);
        log.info(
                "Agentic ChatClient created, tenantId={}, providerId={}, name={}, type={}, baseUrl={}",
                tenantId,
                providerId,
                provider.getName(),
                provider.getProviderType(),
                provider.getBaseUrl());
        return client;
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
                .maxRetries(properties.getTransportMaxRetries())
                .build();
        OpenAIClientAsync asyncClient = OpenAIOkHttpClientAsync.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .maxRetries(properties.getTransportMaxRetries())
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
                .maxRetries(properties.getTransportMaxRetries())
                .build();
        AnthropicClientAsync asyncClient = AnthropicOkHttpClientAsync.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .maxRetries(properties.getTransportMaxRetries())
                .build();
        AnthropicChatModel model = AnthropicChatModel.builder()
                .anthropicClient(syncClient)
                .anthropicClientAsync(asyncClient)
                .build();
        return ChatClient.builder(model).build();
    }

    @PreDestroy
    void shutdownSlotExecutor() {
        slotExecutor.shutdownNow();
    }
}
