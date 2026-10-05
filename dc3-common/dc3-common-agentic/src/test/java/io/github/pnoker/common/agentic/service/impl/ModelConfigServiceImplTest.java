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
package io.github.pnoker.common.agentic.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.repository.ReactiveModelConfigStore;
import io.github.pnoker.common.agentic.repository.ReactiveModelProviderStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ModelConfigServiceImplTest {

    @Mock
    private ReactiveModelConfigStore modelConfigStore;

    @Mock
    private ReactiveModelProviderStore modelProviderStore;

    @Mock
    private io.github.pnoker.common.agentic.config.ChatClientFactory chatClientFactory;

    private ModelConfigServiceImpl service;
    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() throws Exception {
        service = new ModelConfigServiceImpl(
                modelConfigStore, modelProviderStore, new AgenticProperties(), chatClientFactory);
        lenient().when(modelConfigStore.clearCheckProfile(any(), any())).thenReturn(Mono.just(true));
        injectField("fallbackModel", "gpt-4o");
        injectField("fallbackTemperature", 0.7);
        injectField("fallbackMaxTokens", 2048);
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
    }

    @Test
    void saveRejectsBlankModel() {
        ModelConfigBO bo = new ModelConfigBO();
        bo.setModel("   ");
        bo.setProviderId(1L);
        StepVerifier.create(service.add(bo, header))
                .expectErrorSatisfies(error -> {
                    assert error instanceof RequestException;
                    assert error.getMessage().contains("Model is required");
                })
                .verify();
    }

    @Test
    void saveRejectsMissingProvider() {
        ModelConfigBO bo = new ModelConfigBO();
        bo.setModel("gpt-4o");
        bo.setProviderId(0L);
        StepVerifier.create(service.add(bo, header))
                .expectErrorSatisfies(error -> {
                    assert error instanceof RequestException;
                    assert error.getMessage().contains("Provider is required");
                })
                .verify();
    }

    @Test
    void saveRejectsUnknownProviderId() {
        ModelConfigBO bo = config(7L);
        when(modelProviderStore.get(7L, header)).thenReturn(Mono.empty());
        StepVerifier.create(service.add(bo, header))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void saveRejectsTemperatureOutOfRange() {
        ModelConfigBO bo = config(7L);
        bo.setTemperature(2.5);
        StepVerifier.create(service.add(bo, header))
                .expectErrorSatisfies(error -> {
                    assert error instanceof RequestException;
                    assert error.getMessage().contains("Temperature");
                })
                .verify();
        verify(modelProviderStore, never()).get(any(), any());
    }

    @Test
    void saveRejectsMaxTokensZero() {
        ModelConfigBO bo = config(7L);
        bo.setMaxTokens(0);
        StepVerifier.create(service.add(bo, header))
                .expectErrorSatisfies(error -> {
                    assert error instanceof RequestException;
                    assert error.getMessage().contains("Max tokens");
                })
                .verify();
        verify(modelProviderStore, never()).get(any(), any());
    }

    @Test
    void updateRejectsMissingId() {
        ModelConfigBO bo = config(1L);
        StepVerifier.create(service.update(bo, header))
                .expectErrorSatisfies(error -> {
                    assert error instanceof RequestException;
                    assert error.getMessage().contains("Model config ID");
                })
                .verify();
    }

    @Test
    void updateRejectsUnknownConfig() {
        ModelConfigBO bo = config(1L);
        bo.setId(7L);
        when(modelProviderStore.get(1L, header)).thenReturn(Mono.just(provider()));
        when(modelConfigStore.get(7L, header)).thenReturn(Mono.empty());
        StepVerifier.create(service.update(bo, header))
                .expectError(NotFoundException.class)
                .verify();
        verify(modelConfigStore, never()).update(any(), any());
    }

    @Test
    void updateEvictsProviderClientCache() {
        ModelConfigBO existing = storedConfig(7L, 1L, "gpt-4o");
        ModelConfigBO updated = storedConfig(7L, 1L, "gpt-4o");
        when(modelProviderStore.get(1L, header)).thenReturn(Mono.just(provider()));
        when(modelConfigStore.get(7L, header)).thenReturn(Mono.just(existing));
        when(modelConfigStore.update(any(ModelConfigBO.class), any())).thenReturn(Mono.just(updated));

        StepVerifier.create(service.update(configWithId(7L, 1L, "gpt-4o"), header))
                .expectNext(updated)
                .verifyComplete();

        verify(chatClientFactory).evict(1L);
        verify(chatClientFactory, never()).evict(2L);
    }

    @Test
    void updateEvictsBothProvidersWhenConfigIsRebound() {
        ModelConfigBO existing = storedConfig(7L, 1L, "gpt-4o");
        ModelConfigBO updated = storedConfig(7L, 2L, "gpt-4o");
        when(modelProviderStore.get(2L, header)).thenReturn(Mono.just(provider()));
        when(modelConfigStore.get(7L, header)).thenReturn(Mono.just(existing));
        when(modelConfigStore.update(any(ModelConfigBO.class), any())).thenReturn(Mono.just(updated));

        StepVerifier.create(service.update(configWithId(7L, 2L, "gpt-4o"), header))
                .expectNext(updated)
                .verifyComplete();

        verify(chatClientFactory).evict(1L);
        verify(chatClientFactory).evict(2L);
    }

    @Test
    void updateClearsHealthProfileWhenProbeRelevantFieldsChange() {
        ModelConfigBO existing = storedConfig(7L, 1L, "gpt-4o");
        ModelConfigBO updated = storedConfig(7L, 1L, "gpt-4o-mini");
        when(modelProviderStore.get(1L, header)).thenReturn(Mono.just(provider()));
        when(modelConfigStore.get(7L, header)).thenReturn(Mono.just(existing));
        when(modelConfigStore.update(any(ModelConfigBO.class), any())).thenReturn(Mono.just(updated));

        StepVerifier.create(service.update(configWithId(7L, 1L, "gpt-4o-mini"), header))
                .expectNext(updated)
                .verifyComplete();

        verify(modelConfigStore).clearCheckProfile(7L, header);
    }

    @Test
    void deleteEvictsProviderClientCache() {
        when(modelConfigStore.get(42L, header)).thenReturn(Mono.just(storedConfig(42L, 5L, "gpt-4o")));
        when(modelConfigStore.delete(42L, header)).thenReturn(Mono.just(true));

        StepVerifier.create(service.delete(42L, header)).verifyComplete();

        verify(chatClientFactory).evict(5L);
    }

    private ModelConfigBO config(Long providerId) {
        ModelConfigBO config = new ModelConfigBO();
        config.setModel("gpt-4o");
        config.setProviderId(providerId);
        return config;
    }

    private ModelConfigBO configWithId(Long id, Long providerId, String model) {
        ModelConfigBO config = config(providerId);
        config.setId(id);
        config.setModel(model);
        return config;
    }

    private ModelConfigBO storedConfig(Long id, Long providerId, String model) {
        ModelConfigBO config = configWithId(id, providerId, model);
        config.setStream(true);
        config.setToolCall(true);
        config.setVision(false);
        config.setReasoning(false);
        config.setTemperature(0.7);
        config.setMaxTokens(2048);
        config.setTenantId(header.getTenantId());
        return config;
    }

    private ModelProviderBO provider() {
        ModelProviderBO provider = new ModelProviderBO();
        provider.setId(1L);
        provider.setTenantId(header.getTenantId());
        return provider;
    }

    private void injectField(String name, Object value) throws Exception {
        Field field = ModelConfigServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
