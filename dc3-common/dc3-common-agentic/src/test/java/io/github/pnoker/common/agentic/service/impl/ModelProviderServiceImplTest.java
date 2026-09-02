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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.repository.ReactiveModelProviderStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ModelProviderServiceImplTest {

    @Mock
    private ReactiveModelProviderStore modelProviderStore;

    @Mock
    private ChatClientFactory chatClientFactory;

    private ModelProviderServiceImpl service;
    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        service = new ModelProviderServiceImpl(modelProviderStore, chatClientFactory);
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
    }

    @Test
    void saveRejectsNullEntity() {
        StepVerifier.create(service.add(null, header))
                .expectErrorSatisfies(error -> {
                    assert error instanceof RequestException;
                    assert error.getMessage().contains("Provider name");
                })
                .verify();
    }

    @Test
    void saveRejectsBlankName() {
        ModelProviderBO bo = new ModelProviderBO();
        bo.setName("   ");
        bo.setBaseUrl("https://api");
        StepVerifier.create(service.add(bo, header))
                .expectErrorSatisfies(error -> {
                    assert error instanceof RequestException;
                    assert error.getMessage().contains("Provider name");
                })
                .verify();
    }

    @Test
    void saveRejectsBlankBaseUrl() {
        ModelProviderBO bo = new ModelProviderBO();
        bo.setName("Anthropic");
        bo.setBaseUrl("  ");
        StepVerifier.create(service.add(bo, header))
                .expectErrorSatisfies(error -> {
                    assert error instanceof RequestException;
                    assert error.getMessage().contains("base URL");
                })
                .verify();
    }

    @Test
    void updateRejectsMissingId() {
        ModelProviderBO bo = new ModelProviderBO();
        bo.setName("Anthropic");
        bo.setBaseUrl("https://api");
        StepVerifier.create(service.update(bo, header))
                .expectErrorSatisfies(error -> {
                    assert error instanceof RequestException;
                    assert error.getMessage().contains("Provider ID");
                })
                .verify();
    }

    @Test
    void updateRejectsUnknownProvider() {
        ModelProviderBO bo = provider(7L);
        when(modelProviderStore.get(7L, header)).thenReturn(Mono.empty());
        StepVerifier.create(service.update(bo, header))
                .expectError(NotFoundException.class)
                .verify();
        verify(modelProviderStore, never()).update(any(), any());
        verify(chatClientFactory, never()).evict(any());
    }

    @Test
    void updateInvalidatesChatClientCacheForId() {
        ModelProviderBO existing = provider(7L);
        ModelProviderBO updated = provider(7L);
        when(modelProviderStore.get(7L, header)).thenReturn(Mono.just(existing));
        when(modelProviderStore.update(any(ModelProviderBO.class), any())).thenReturn(Mono.just(updated));
        StepVerifier.create(service.update(updated, header)).expectNext(updated).verifyComplete();
        verify(chatClientFactory).evict(7L);
    }

    @Test
    void deleteEvictsCachedClient() {
        when(modelProviderStore.delete(42L, header)).thenReturn(Mono.just(true));
        StepVerifier.create(service.delete(42L, header)).verifyComplete();
        verify(chatClientFactory).evict(42L);
    }

    private ModelProviderBO provider(Long id) {
        ModelProviderBO provider = new ModelProviderBO();
        provider.setId(id);
        provider.setName("Anthropic");
        provider.setBaseUrl("https://api");
        provider.setTenantId(header.getTenantId());
        return provider;
    }
}
