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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.data.dal.MessageManager;
import io.github.pnoker.common.data.dal.NotifyChannelBindManager;
import io.github.pnoker.common.data.dal.NotifyChannelManager;
import io.github.pnoker.common.data.dal.NotifyManager;
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.builder.MessageBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBindBuilder;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBuilder;
import io.github.pnoker.common.data.entity.model.MessageDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.model.NotifyDO;
import io.github.pnoker.common.data.entity.property.AlarmCacheProperties;
import io.github.pnoker.common.enums.NotifyChannelTypeFlagEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyConfigCacheTest {

    @Mock
    private NotifyManager notifyManager;
    @Mock
    private NotifyBuilder notifyBuilder;
    @Mock
    private MessageManager messageManager;
    @Mock
    private MessageBuilder messageBuilder;
    @Mock
    private NotifyChannelManager notifyChannelManager;
    @Mock
    private NotifyChannelBuilder notifyChannelBuilder;
    @Mock
    private NotifyChannelBindManager notifyChannelBindManager;
    @Mock
    private NotifyChannelBindBuilder notifyChannelBindBuilder;

    private NotifyConfigCache cache;

    @BeforeEach
    void setUp() {
        cache = new NotifyConfigCache(notifyManager, notifyBuilder,
                messageManager, messageBuilder,
                notifyChannelManager, notifyChannelBuilder,
                notifyChannelBindManager, notifyChannelBindBuilder,
                new AlarmCacheProperties());
    }

    @Test
    void cachesNotifyAcrossLookups() {
        NotifyDO entity = new NotifyDO();
        entity.setId(1L);
        NotifyBO bo = new NotifyBO();
        bo.setId(1L);
        when(notifyManager.getById(1L)).thenReturn(entity);
        when(notifyBuilder.buildBOByDO(entity)).thenReturn(bo);

        NotifyBO first = cache.findNotify(1L);
        NotifyBO second = cache.findNotify(1L);

        assertThat(first).isSameAs(bo);
        assertThat(second).isSameAs(bo);
        verify(notifyManager, times(1)).getById(1L);
    }

    @Test
    void invalidateNotifyForcesReload() {
        NotifyDO entity = new NotifyDO();
        when(notifyManager.getById(1L)).thenReturn(entity);
        when(notifyBuilder.buildBOByDO(any())).thenReturn(new NotifyBO());

        cache.findNotify(1L);
        cache.invalidateNotify(1L);
        cache.findNotify(1L);

        verify(notifyManager, times(2)).getById(1L);
    }

    @Test
    void cachesMessageAcrossLookups() {
        MessageDO entity = new MessageDO();
        when(messageManager.getById(2L)).thenReturn(entity);
        when(messageBuilder.buildBOByDO(any())).thenReturn(new MessageBO());

        cache.findMessage(2L);
        cache.findMessage(2L);

        verify(messageManager, times(1)).getById(2L);
    }

    @Test
    void findChannelEnforcesTenantScopeAtCallSite() {
        NotifyChannelDO entity = new NotifyChannelDO();
        entity.setTenantId(7L);
        entity.setChannelTypeFlag((byte) 0);
        when(notifyChannelManager.getById(3L)).thenReturn(entity);
        NotifyChannelBO bo = new NotifyChannelBO();
        bo.setTenantId(7L);
        bo.setChannelTypeFlag(NotifyChannelTypeFlagEnum.FEISHU_BOT);
        when(notifyChannelBuilder.buildBOByDO(entity)).thenReturn(bo);

        // Same tenant id → return; different tenant id → null even on cache hit.
        assertThat(cache.findChannel(3L, 7L)).isSameAs(bo);
        assertThat(cache.findChannel(3L, 9L)).isNull();
    }

    @Test
    void findChannelReturnsNullForMissingChannelTypeFlag() {
        NotifyChannelDO entity = new NotifyChannelDO();
        entity.setTenantId(7L);
        // channelTypeFlag intentionally null
        when(notifyChannelManager.getById(3L)).thenReturn(entity);

        assertThat(cache.findChannel(3L, 7L)).isNull();
    }

    @Test
    void invalidateAllDropsEverySection() {
        when(notifyManager.getById(1L)).thenReturn(new NotifyDO());
        when(notifyBuilder.buildBOByDO(any())).thenReturn(new NotifyBO());
        when(messageManager.getById(2L)).thenReturn(new MessageDO());
        when(messageBuilder.buildBOByDO(any())).thenReturn(new MessageBO());

        cache.findNotify(1L);
        cache.findMessage(2L);
        cache.invalidateAll();
        cache.findNotify(1L);
        cache.findMessage(2L);

        verify(notifyManager, times(2)).getById(1L);
        verify(messageManager, times(2)).getById(2L);
    }

    @Test
    void rejectsZeroAndNullIds() {
        // Default ids never query; this prevents hot rules with default-zero
        // notify/message/channel ids from continually polling the database for
        // a row that doesn't exist.
        cache.findNotify(0L);
        cache.findNotify(null);
        cache.findMessage(0L);
        cache.findMessage(null);
        cache.findChannel(0L, 7L);
        cache.findChannel(null, 7L);

        verifyNoInteractions(notifyManager, messageManager, notifyChannelManager);
    }

}
