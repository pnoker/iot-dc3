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

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.dal.NotifyChannelManager;
import io.github.pnoker.common.data.dal.NotifyHistoryManager;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBuilder;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;
import io.github.pnoker.common.entity.dto.NotifyTaskDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeFlagEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyWorkerTest {

    @Mock
    private NotifyChannelManager notifyChannelManager;

    @Mock
    private NotifyChannelBuilder notifyChannelBuilder;

    @Mock
    private NotifyChannelAdapterRegistry notifyChannelAdapterRegistry;

    @Mock
    private NotifyHistoryManager notifyHistoryManager;

    @Mock
    private NotifyTaskSender notifyTaskSender;

    @Mock
    private NotifyChannelAdapter adapter;

    @Mock
    private Channel channel;

    @InjectMocks
    private NotifyWorker worker;

    private Message message;

    @BeforeEach
    void setUp() {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(11L);
        message = new Message(new byte[0], props);
    }

    private static NotifyTaskDTO task(int retry) {
        return NotifyTaskDTO.builder()
                .notifyHistoryId(50L)
                .tenantId(7L)
                .channelId(2L)
                .channelTypeFlag(NotifyChannelTypeFlagEnum.WEBHOOK.getIndex())
                .payloadType("webhook-json")
                .payload(Map.of("title", "test"))
                .retryCount(retry)
                .build();
    }

    private void stubChannel(boolean enabled, NotifyChannelTypeFlagEnum type) {
        NotifyChannelDO entityDO = new NotifyChannelDO();
        entityDO.setId(2L);
        entityDO.setTenantId(7L);
        entityDO.setChannelTypeFlag(type.getIndex());
        when(notifyChannelManager.getById(2L)).thenReturn(entityDO);
        NotifyChannelBO bo = new NotifyChannelBO();
        bo.setId(2L);
        bo.setTenantId(7L);
        bo.setChannelTypeFlag(type);
        bo.setEnableFlag(enabled ? EnableFlagEnum.ENABLE : EnableFlagEnum.DISABLE);
        bo.setCredentialRef("secret:webhook:test");
        when(notifyChannelBuilder.buildBOByDO(entityDO)).thenReturn(bo);
    }

    @Test
    void marksHistorySuccessAndAcksOnSuccess() throws Exception {
        stubChannel(true, NotifyChannelTypeFlagEnum.WEBHOOK);
        when(notifyChannelAdapterRegistry.find(NotifyChannelTypeFlagEnum.WEBHOOK)).thenReturn(Optional.of(adapter));
        when(adapter.send(any(), any())).thenReturn(NotifySendResult.success("https://hook", 200, "OK", Map.of()));

        worker.onNotifyTask(channel, message, task(0));

        ArgumentCaptor<NotifyHistoryDO> captor = ArgumentCaptor.forClass(NotifyHistoryDO.class);
        verify(notifyHistoryManager).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(50L);
        assertThat(captor.getValue().getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.SUCCESS.getIndex());
        verify(channel).basicAck(eq(11L), eq(false));
        verify(notifyTaskSender, never()).publish(any());
    }

    @Test
    void requeuesAsRetryingOnFirstFailure() throws Exception {
        stubChannel(true, NotifyChannelTypeFlagEnum.WEBHOOK);
        when(notifyChannelAdapterRegistry.find(NotifyChannelTypeFlagEnum.WEBHOOK)).thenReturn(Optional.of(adapter));
        when(adapter.send(any(), any())).thenReturn(NotifySendResult.failed("https://hook", "503"));

        worker.onNotifyTask(channel, message, task(0));

        ArgumentCaptor<NotifyHistoryDO> captor = ArgumentCaptor.forClass(NotifyHistoryDO.class);
        verify(notifyHistoryManager).updateById(captor.capture());
        assertThat(captor.getValue().getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.RETRYING.getIndex());
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
        // Retry task is republished with incremented retry count
        ArgumentCaptor<NotifyTaskDTO> taskCaptor = ArgumentCaptor.forClass(NotifyTaskDTO.class);
        verify(notifyTaskSender).publish(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getRetryCount()).isEqualTo(1);
        verify(channel).basicAck(eq(11L), eq(false));
    }

    @Test
    void terminatesAsFailedAfterMaxAttempts() throws Exception {
        stubChannel(true, NotifyChannelTypeFlagEnum.WEBHOOK);
        when(notifyChannelAdapterRegistry.find(NotifyChannelTypeFlagEnum.WEBHOOK)).thenReturn(Optional.of(adapter));
        when(adapter.send(any(), any())).thenReturn(NotifySendResult.failed("https://hook", "503"));

        // retry already at MAX_ATTEMPTS - 1; one more failure should terminate.
        worker.onNotifyTask(channel, message, task(NotifyWorker.MAX_ATTEMPTS - 1));

        ArgumentCaptor<NotifyHistoryDO> captor = ArgumentCaptor.forClass(NotifyHistoryDO.class);
        verify(notifyHistoryManager).updateById(captor.capture());
        assertThat(captor.getValue().getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.FAILED.getIndex());
        verify(notifyTaskSender, never()).publish(any());
        verify(channel).basicAck(eq(11L), eq(false));
    }

    @Test
    void marksHistorySkippedWhenChannelDisabled() throws Exception {
        stubChannel(false, NotifyChannelTypeFlagEnum.WEBHOOK);

        worker.onNotifyTask(channel, message, task(0));

        ArgumentCaptor<NotifyHistoryDO> captor = ArgumentCaptor.forClass(NotifyHistoryDO.class);
        verify(notifyHistoryManager).updateById(captor.capture());
        assertThat(captor.getValue().getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.SKIPPED.getIndex());
        // Adapter must not be invoked for a disabled channel
        verifyNoInteractions(adapter);
        verify(channel).basicAck(eq(11L), eq(false));
    }

    @Test
    void rejectsTaskWithoutHistoryId() throws Exception {
        NotifyTaskDTO bad = NotifyTaskDTO.builder().channelId(2L).build();
        worker.onNotifyTask(channel, message, bad);
        verify(channel).basicReject(eq(11L), eq(false));
        verify(notifyHistoryManager, never()).updateById(any());
    }

    @Test
    void marksHistoryFailedWhenAdapterMissing() throws Exception {
        stubChannel(true, NotifyChannelTypeFlagEnum.FEISHU_BOT);
        when(notifyChannelAdapterRegistry.find(NotifyChannelTypeFlagEnum.FEISHU_BOT)).thenReturn(Optional.empty());

        worker.onNotifyTask(channel, message, NotifyTaskDTO.builder()
                .notifyHistoryId(50L).tenantId(7L).channelId(2L)
                .channelTypeFlag(NotifyChannelTypeFlagEnum.FEISHU_BOT.getIndex())
                .build());

        ArgumentCaptor<NotifyHistoryDO> captor = ArgumentCaptor.forClass(NotifyHistoryDO.class);
        verify(notifyHistoryManager).updateById(captor.capture());
        assertThat(captor.getValue().getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.FAILED.getIndex());
        verify(channel).basicAck(eq(11L), eq(false));
    }

}
