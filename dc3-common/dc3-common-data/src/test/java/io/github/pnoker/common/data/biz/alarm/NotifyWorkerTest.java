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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.repository.ReactiveNotifyHistoryStore;
import io.github.pnoker.common.entity.dto.NotifyTaskDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class NotifyWorkerTest {

    @Mock
    private NotifyConfigCache notifyConfigCache;

    @Mock
    private NotifyChannelAdapterRegistry notifyChannelAdapterRegistry;

    @Mock
    private NotifyChannelAdapter adapter;

    @Mock
    private ReactiveNotifyHistoryStore notifyHistoryStore;

    @Mock
    private NotifyTaskSender notifyTaskSender;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private NotifyWorker worker;

    @Test
    void marksSuccessAndAcks() {
        stubChannel(true);
        when(notifyChannelAdapterRegistry.find(NotifyChannelTypeEnum.WEBHOOK)).thenReturn(Optional.of(adapter));
        when(adapter.send(any(), any())).thenReturn(Mono.just(NotifySendResult.success("target", 200, "OK", Map.of())));
        when(notifyHistoryStore.updateDelivery(anyLong(), anyLong(), anyByte(), any(), any(), any(), anyInt()))
                .thenReturn(Mono.just(true));

        worker.onNotifyTask(new MqReceived<>(task(0), Map.of(), false), ack).block();

        verify(notifyHistoryStore).updateDelivery(anyLong(), anyLong(), anyByte(), any(), any(), any(), anyInt());
        verifyNoInteractions(ack);
        verify(notifyTaskSender, never()).publish(any());
    }

    @Test
    void retriesFailureBeforeMaxAttempts() {
        stubChannel(true);
        when(notifyChannelAdapterRegistry.find(NotifyChannelTypeEnum.WEBHOOK)).thenReturn(Optional.of(adapter));
        when(adapter.send(any(), any())).thenReturn(Mono.just(NotifySendResult.failed("target", "503")));
        when(notifyHistoryStore.updateDelivery(anyLong(), anyLong(), anyByte(), any(), any(), any(), anyInt()))
                .thenReturn(Mono.just(true));

        worker.onNotifyTask(new MqReceived<>(task(0), Map.of(), false), ack).block();

        ArgumentCaptor<NotifyTaskDTO> captor = ArgumentCaptor.forClass(NotifyTaskDTO.class);
        verify(notifyTaskSender).publish(captor.capture());
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
        verifyNoInteractions(ack);
    }

    @Test
    void rejectsMissingTenantBeforeDatabaseAccess() {
        NotifyTaskDTO invalid =
                NotifyTaskDTO.builder().notifyHistoryId(50L).channelId(2L).build();
        worker.onNotifyTask(new MqReceived<>(invalid, Map.of(), false), ack).block();
        verify(ack).reject(false);
        verifyNoInteractions(notifyHistoryStore);
    }

    @Test
    void rejectsWhenHistoryUpdateAffectsNoRows() {
        stubChannel(true);
        when(notifyChannelAdapterRegistry.find(NotifyChannelTypeEnum.WEBHOOK)).thenReturn(Optional.of(adapter));
        when(adapter.send(any(), any())).thenReturn(Mono.just(NotifySendResult.success("target", 200, "OK", Map.of())));
        when(notifyHistoryStore.updateDelivery(anyLong(), anyLong(), anyByte(), any(), any(), any(), anyInt()))
                .thenReturn(Mono.just(false));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> worker.onNotifyTask(new MqReceived<>(task(0), Map.of(), false), ack)
                                .block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("notify history row not found");
        verifyNoInteractions(ack);
    }

    private NotifyTaskDTO task(int retry) {
        return NotifyTaskDTO.builder()
                .notifyHistoryId(50L)
                .tenantId(7L)
                .channelId(2L)
                .channelTypeFlag(NotifyChannelTypeEnum.WEBHOOK.getIndex())
                .payloadType("text")
                .payload(Map.of("text", "hello"))
                .retryCount(retry)
                .build();
    }

    private void stubChannel(boolean enabled) {
        NotifyChannelBO channel = new NotifyChannelBO();
        channel.setId(2L);
        channel.setTenantId(7L);
        channel.setEnableFlag(enabled ? EnableFlagEnum.ENABLE : EnableFlagEnum.DISABLE);
        channel.setChannelTypeFlag(NotifyChannelTypeEnum.WEBHOOK);
        channel.setCredentialRef("target");
        when(notifyConfigCache.getChannel(2L, 7L)).thenReturn(Mono.just(channel));
    }
}
