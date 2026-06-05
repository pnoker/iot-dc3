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

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.dal.ModelConfigManager;
import io.github.pnoker.common.agentic.dal.ModelProviderManager;
import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.builder.ModelConfigBuilder;
import io.github.pnoker.common.agentic.entity.model.ModelProviderDO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validation-path coverage of {@link ModelConfigServiceImpl}. Tests that exercise the
 * full save/update/list flows are deferred to integration tests because they build
 * MyBatis-Plus {@code Wrappers.lambdaQuery()} chains that need the entity cache.
 */
@ExtendWith(MockitoExtension.class)
class ModelConfigServiceImplTest {

    @Mock
    private ModelConfigManager modelConfigManager;

    @Mock
    private ModelProviderManager modelProviderManager;

    @Mock
    private ModelConfigBuilder modelConfigBuilder;

    private ModelConfigServiceImpl service;
    private RequestHeader.UserHeader header;

    @BeforeEach
    void setUp() throws Exception {
        service = new ModelConfigServiceImpl(modelConfigManager, modelProviderManager, modelConfigBuilder,
                new AgenticProperties());
        injectField("fallbackModel", "gpt-4o");
        injectField("fallbackTemperature", 0.7);
        injectField("fallbackMaxTokens", 2048);
        header = new RequestHeader.UserHeader();
        header.setTenantId(1L);
        header.setUserId(2L);
        header.setUserName("admin");
    }

    @Test
    void saveRejectsBlankModel() {
        ModelConfigBO bo = new ModelConfigBO();
        bo.setModel("   ");
        bo.setProviderId(1L);
        assertThatThrownBy(() -> service.add(bo, header)).isInstanceOf(RequestException.class)
                .hasMessageContaining("Model is required");
    }

    @Test
    void saveRejectsMissingProvider() {
        ModelConfigBO bo = new ModelConfigBO();
        bo.setModel("gpt-4o");
        bo.setProviderId(0L);
        assertThatThrownBy(() -> service.add(bo, header)).isInstanceOf(RequestException.class)
                .hasMessageContaining("Provider is required");
    }

    @Test
    void saveRejectsUnknownProviderId() {
        ModelConfigBO bo = new ModelConfigBO();
        bo.setModel("gpt-4o");
        bo.setProviderId(7L);
        when(modelProviderManager.getById(7L)).thenReturn(null);
        assertThatThrownBy(() -> service.add(bo, header)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Provider does not exist");
    }

    @Test
    void saveRejectsTemperatureOutOfRange() {
        ModelConfigBO bo = new ModelConfigBO();
        bo.setModel("gpt-4o");
        bo.setProviderId(7L);
        bo.setTemperature(2.5);
        when(modelProviderManager.getById(7L)).thenReturn(new ModelProviderDO());
        assertThatThrownBy(() -> service.add(bo, header)).isInstanceOf(RequestException.class)
                .hasMessageContaining("Temperature");
    }

    @Test
    void saveRejectsMaxTokensZero() {
        ModelConfigBO bo = new ModelConfigBO();
        bo.setModel("gpt-4o");
        bo.setProviderId(7L);
        bo.setMaxTokens(0);
        when(modelProviderManager.getById(7L)).thenReturn(new ModelProviderDO());
        assertThatThrownBy(() -> service.add(bo, header)).isInstanceOf(RequestException.class)
                .hasMessageContaining("Max tokens");
    }

    @Test
    void updateRejectsMissingId() {
        ModelConfigBO bo = new ModelConfigBO();
        bo.setModel("gpt-4o");
        assertThatThrownBy(() -> service.update(bo, header)).isInstanceOf(RequestException.class)
                .hasMessageContaining("Model config ID");
    }

    @Test
    void updateRejectsUnknownConfig() {
        ModelConfigBO bo = new ModelConfigBO();
        bo.setId(7L);
        bo.setModel("gpt-4o");
        bo.setProviderId(1L);
        when(modelProviderManager.getById(1L)).thenReturn(new ModelProviderDO());
        when(modelConfigManager.getById(7L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(bo, header)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Model config does not exist");
    }

    @Test
    void removeDelegatesToManager() {
        service.delete(42L);
        verify(modelConfigManager).removeById(42L);
    }

    private void injectField(String name, Object value) throws Exception {
        Field field = ModelConfigServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
