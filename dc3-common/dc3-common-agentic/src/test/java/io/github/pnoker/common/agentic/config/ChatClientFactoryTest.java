/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.agentic.config;

import io.github.pnoker.common.agentic.repository.ReactiveModelConfigStore;
import io.github.pnoker.common.agentic.repository.ReactiveModelProviderStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChatClientFactoryTest {

    @Mock
    private ReactiveModelProviderStore modelProviderStore;

    @Mock
    private ReactiveModelConfigStore modelConfigStore;

    @Mock
    private ChatClient.Builder fallbackBuilder;

    private ChatClientFactory factory;
    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() throws Exception {
        AgenticProperties properties = new AgenticProperties();
        factory = new ChatClientFactory(modelProviderStore, modelConfigStore, fallbackBuilder, properties);
        injectField("fallbackModel", "gpt-4o");
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
        lenient().when(modelConfigStore.findDefault(header)).thenReturn(reactor.core.publisher.Mono.empty());
        lenient().when(modelConfigStore.findByModel("gpt-4o", header)).thenReturn(reactor.core.publisher.Mono.empty());
        lenient().when(modelConfigStore.findByModel("unknown-model", header)).thenReturn(reactor.core.publisher.Mono.empty());
    }

    @Test
    void resolveModelUsesReactiveFallbackWhenNoModelConfigExists() {
        StepVerifier.create(factory.resolveModelReactive("  gpt-4o  ", header))
                .expectNext("gpt-4o").verifyComplete();
        StepVerifier.create(factory.resolveModelReactive("unknown-model", header))
                .expectNext("gpt-4o").verifyComplete();
    }

    @Test
    void supportsToolCallUsesFallbackCapabilityWhenNoModelConfigExists() {
        StepVerifier.create(factory.supportsToolCallReactive("gpt-4o", header))
                .expectNext(true).verifyComplete();
        StepVerifier.create(factory.supportsToolCallReactive("unknown-model", header))
                .expectNext(false).verifyComplete();
    }

    @Test
    void evictIsSafeForUnknownProviderId() {
        factory.evict(999L);
        assertThat(factory).isNotNull();
    }

    private void injectField(String name, Object value) throws Exception {
        Field field = ChatClientFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(factory, value);
    }
}
