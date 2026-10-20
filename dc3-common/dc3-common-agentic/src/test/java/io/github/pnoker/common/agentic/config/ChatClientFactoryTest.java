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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.repository.ReactiveModelConfigStore;
import io.github.pnoker.common.agentic.repository.ReactiveModelProviderStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChatClientFactoryTest {

    @Mock
    private ReactiveModelProviderStore modelProviderStore;

    @Mock
    private ReactiveModelConfigStore modelConfigStore;

    @Mock
    private ChatClient.Builder fallbackBuilder;

    private AgenticProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private ChatClientFactory factory;
    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() throws Exception {
        properties = new AgenticProperties();
        meterRegistry = new SimpleMeterRegistry();
        factory =
                new ChatClientFactory(modelProviderStore, modelConfigStore, fallbackBuilder, properties, meterRegistry);
        injectField("fallbackModel", "gpt-4o");
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
        lenient().when(modelConfigStore.getDefault(header)).thenReturn(Mono.empty());
        lenient().when(modelConfigStore.getByModel("gpt-4o", header)).thenReturn(Mono.empty());
        lenient().when(modelConfigStore.getByModel("unknown-model", header)).thenReturn(Mono.empty());
    }

    @Test
    void fallbackDisabledByDefaultFailsWithUnavailableInsteadOfSilentFallback() {
        StepVerifier.create(factory.resolveModelReactive("  gpt-4o  ", header))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) error).getStatusCode())
                            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                })
                .verify();
        StepVerifier.create(factory.resolveModelReactive("unknown-model", header))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void explicitlyEnabledFallbackResolvesTheConfiguredModel() {
        properties.setTransportFallbackEnabled(true);
        StepVerifier.create(factory.resolveModelReactive("  gpt-4o  ", header))
                .expectNext("gpt-4o")
                .verifyComplete();
        StepVerifier.create(factory.resolveModelReactive("unknown-model", header))
                .expectNext("gpt-4o")
                .verifyComplete();
    }

    @Test
    void resolvedTenantConfigShortCircuitsTheFallbackGate() {
        stubTenantModel("tenant-model");

        StepVerifier.create(factory.resolveModelReactive("tenant-model", header))
                .expectNext("tenant-model")
                .verifyComplete();
    }

    @Test
    void supportsToolCallUsesFallbackCapabilityWhenNoModelConfigExists() {
        StepVerifier.create(factory.supportsToolCallReactive("gpt-4o", header))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(factory.supportsToolCallReactive("unknown-model", header))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void evictIsSafeForUnknownProviderId() {
        factory.evict(999L);
        assertThat(factory).isNotNull();
    }

    @Test
    void configCacheEntriesExpireAfterTheTtl() throws Exception {
        properties.setTransportCacheTtlSeconds(1);
        factory =
                new ChatClientFactory(modelProviderStore, modelConfigStore, fallbackBuilder, properties, meterRegistry);
        injectField("fallbackModel", "gpt-4o");
        stubTenantModel("tenant-model");

        StepVerifier.create(factory.resolveModelReactive("tenant-model", header))
                .expectNext("tenant-model")
                .verifyComplete();
        StepVerifier.create(factory.resolveModelReactive("tenant-model", header))
                .expectNext("tenant-model")
                .verifyComplete();
        org.mockito.Mockito.verify(modelConfigStore, org.mockito.Mockito.times(1))
                .getByModel("tenant-model", header);

        Thread.sleep(1200);

        StepVerifier.create(factory.resolveModelReactive("tenant-model", header))
                .expectNext("tenant-model")
                .verifyComplete();
        org.mockito.Mockito.verify(modelConfigStore, org.mockito.Mockito.times(2))
                .getByModel("tenant-model", header);
    }

    @Test
    void uncachedModelsAreThrottledByTheGlobalSemaphore() throws Exception {
        // No cached config: the acquire must hit the global semaphore (cap 1)
        // instead of running unthrottled, and overflow must fail fast.
        properties.setTransportGlobalMaxConcurrentRequests(1);
        properties.setTransportAcquireTimeoutSeconds(1);
        factory =
                new ChatClientFactory(modelProviderStore, modelConfigStore, fallbackBuilder, properties, meterRegistry);
        injectField("fallbackModel", "gpt-4o");

        ChatClientFactory.InFlightLease lease =
                factory.acquireInFlightSlot("unknown-model", 1L).block(Duration.ofSeconds(5));
        assertThat(lease).isNotNull();
        assertThat(lease.semaphore()).isNotNull();

        StepVerifier.create(factory.acquireInFlightSlot("unknown-model", 1L))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) error).getStatusCode())
                            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                })
                .verify(Duration.ofSeconds(5));

        factory.releaseInFlightSlot(lease);
        StepVerifier.create(factory.acquireInFlightSlot("unknown-model", 1L))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void globalAndProviderPermitGaugesAreRegistered() {
        assertThat(meterRegistry.get("agentic.transport.global.permits").gauge().value())
                .isEqualTo(16.0);
    }

    private void stubTenantModel(String model) {
        ModelConfigBO config = new ModelConfigBO();
        config.setModel(model);
        config.setProviderId(7L);
        config.setTenantId(1L);
        config.setDefaultFlag(DefaultFlagEnum.NOT_DEFAULT);
        io.github.pnoker.common.agentic.entity.bo.ModelProviderBO provider =
                new io.github.pnoker.common.agentic.entity.bo.ModelProviderBO();
        provider.setId(7L);
        provider.setTenantId(1L);
        lenient().when(modelConfigStore.getByModel(model, header)).thenReturn(Mono.just(config));
        lenient().when(modelProviderStore.get(7L, header)).thenReturn(Mono.just(provider));
    }

    private void injectField(String name, Object value) throws Exception {
        Field field = ChatClientFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(factory, value);
    }
}
