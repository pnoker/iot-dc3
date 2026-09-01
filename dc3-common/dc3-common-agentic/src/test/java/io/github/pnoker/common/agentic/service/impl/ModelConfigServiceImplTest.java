/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.agentic.service.impl;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.repository.ReactiveModelConfigStore;
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

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelConfigServiceImplTest {

    @Mock
    private ReactiveModelConfigStore modelConfigStore;

    @Mock
    private ReactiveModelProviderStore modelProviderStore;

    private ModelConfigServiceImpl service;
    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() throws Exception {
        service = new ModelConfigServiceImpl(modelConfigStore, modelProviderStore, new AgenticProperties());
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
        StepVerifier.create(service.add(bo, header)).expectErrorSatisfies(error -> {
            assert error instanceof RequestException;
            assert error.getMessage().contains("Model is required");
        }).verify();
    }

    @Test
    void saveRejectsMissingProvider() {
        ModelConfigBO bo = new ModelConfigBO();
        bo.setModel("gpt-4o");
        bo.setProviderId(0L);
        StepVerifier.create(service.add(bo, header)).expectErrorSatisfies(error -> {
            assert error instanceof RequestException;
            assert error.getMessage().contains("Provider is required");
        }).verify();
    }

    @Test
    void saveRejectsUnknownProviderId() {
        ModelConfigBO bo = config(7L);
        when(modelProviderStore.get(7L, header)).thenReturn(Mono.empty());
        StepVerifier.create(service.add(bo, header)).expectError(NotFoundException.class).verify();
    }

    @Test
    void saveRejectsTemperatureOutOfRange() {
        ModelConfigBO bo = config(7L);
        bo.setTemperature(2.5);
        StepVerifier.create(service.add(bo, header)).expectErrorSatisfies(error -> {
            assert error instanceof RequestException;
            assert error.getMessage().contains("Temperature");
        }).verify();
        verify(modelProviderStore, never()).get(any(), any());
    }

    @Test
    void saveRejectsMaxTokensZero() {
        ModelConfigBO bo = config(7L);
        bo.setMaxTokens(0);
        StepVerifier.create(service.add(bo, header)).expectErrorSatisfies(error -> {
            assert error instanceof RequestException;
            assert error.getMessage().contains("Max tokens");
        }).verify();
        verify(modelProviderStore, never()).get(any(), any());
    }

    @Test
    void updateRejectsMissingId() {
        ModelConfigBO bo = config(1L);
        StepVerifier.create(service.update(bo, header)).expectErrorSatisfies(error -> {
            assert error instanceof RequestException;
            assert error.getMessage().contains("Model config ID");
        }).verify();
    }

    @Test
    void updateRejectsUnknownConfig() {
        ModelConfigBO bo = config(1L);
        bo.setId(7L);
        when(modelProviderStore.get(1L, header)).thenReturn(Mono.just(provider()));
        when(modelConfigStore.get(7L, header)).thenReturn(Mono.empty());
        StepVerifier.create(service.update(bo, header)).expectError(NotFoundException.class).verify();
        verify(modelConfigStore, never()).update(any(), any());
    }

    private ModelConfigBO config(Long providerId) {
        ModelConfigBO config = new ModelConfigBO();
        config.setModel("gpt-4o");
        config.setProviderId(providerId);
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
