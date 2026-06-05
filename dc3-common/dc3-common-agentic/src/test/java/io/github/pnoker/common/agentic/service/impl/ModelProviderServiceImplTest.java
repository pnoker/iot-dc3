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

import io.github.pnoker.common.agentic.config.ChatClientFactory;
import io.github.pnoker.common.agentic.dal.ModelProviderManager;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.builder.ModelProviderBuilder;
import io.github.pnoker.common.agentic.entity.model.ModelProviderDO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelProviderServiceImplTest {

    @Mock
    private ModelProviderManager modelProviderManager;

    @Mock
    private ModelProviderBuilder modelProviderBuilder;

    @Mock
    private ChatClientFactory chatClientFactory;

    private ModelProviderServiceImpl service;
    private RequestHeader.UserHeader header;

    @BeforeEach
    void setUp() {
        service = new ModelProviderServiceImpl(modelProviderManager, modelProviderBuilder, chatClientFactory);
        header = new RequestHeader.UserHeader();
        header.setTenantId(1L);
        header.setUserId(2L);
        header.setUserName("admin");
    }

    @Test
    void saveRejectsNullEntity() {
        assertThatThrownBy(() -> service.add(null, header)).isInstanceOf(RequestException.class)
                .hasMessageContaining("Provider name");
    }

    @Test
    void saveRejectsBlankName() {
        ModelProviderBO bo = new ModelProviderBO();
        bo.setName("   ");
        bo.setBaseUrl("https://api");
        assertThatThrownBy(() -> service.add(bo, header)).isInstanceOf(RequestException.class)
                .hasMessageContaining("Provider name");
    }

    @Test
    void saveRejectsBlankBaseUrl() {
        ModelProviderBO bo = new ModelProviderBO();
        bo.setName("Anthropic");
        bo.setBaseUrl("  ");
        assertThatThrownBy(() -> service.add(bo, header)).isInstanceOf(RequestException.class)
                .hasMessageContaining("base URL");
    }

    @Test
    void updateRejectsMissingId() {
        ModelProviderBO bo = new ModelProviderBO();
        bo.setName("Anthropic");
        bo.setBaseUrl("https://api");
        assertThatThrownBy(() -> service.update(bo, header)).isInstanceOf(RequestException.class)
                .hasMessageContaining("Provider ID");
    }

    @Test
    void updateRejectsUnknownProvider() {
        ModelProviderBO bo = new ModelProviderBO();
        bo.setId(7L);
        bo.setName("Anthropic");
        bo.setBaseUrl("https://api");
        when(modelProviderManager.getById(7L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(bo, header)).isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Provider does not exist");
        verify(modelProviderManager, never()).updateById(any(ModelProviderDO.class));
        verify(chatClientFactory, never()).evict(any());
    }

    @Test
    void updateInvalidatesChatClientCacheForId() {
        ModelProviderBO bo = new ModelProviderBO();
        bo.setId(7L);
        bo.setName("Anthropic");
        bo.setBaseUrl("https://api");

        ModelProviderDO existingDO = new ModelProviderDO();
        existingDO.setId(7L);
        when(modelProviderManager.getById(7L)).thenReturn(existingDO);
        when(modelProviderBuilder.buildBOByDO(existingDO)).thenReturn(bo);
        ModelProviderDO mappedDO = new ModelProviderDO();
        mappedDO.setId(7L);
        when(modelProviderBuilder.buildDOByBO(any(ModelProviderBO.class))).thenReturn(mappedDO);
        when(modelProviderBuilder.buildBOByDO(mappedDO)).thenReturn(bo);

        service.update(bo, header);

        verify(modelProviderManager).updateById(mappedDO);
        verify(chatClientFactory).evict(7L);
    }

    @Test
    void removeEvictsCachedClient() {
        service.delete(42L);
        verify(modelProviderManager).removeById(42L);
        verify(chatClientFactory).evict(42L);
    }
}
