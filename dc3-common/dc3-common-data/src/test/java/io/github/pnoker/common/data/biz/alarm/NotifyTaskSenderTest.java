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

import io.github.pnoker.common.entity.dto.NotifyTaskDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyTaskSenderTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private TopicExchange alarmExchange;

    @InjectMocks
    private NotifyTaskSender sender;

    @Test
    void publishesWithChannelTypedRoutingKey() {
        when(alarmExchange.getName()).thenReturn("dc3.e.alarm");
        NotifyTaskDTO task = NotifyTaskDTO.builder()
                .notifyHistoryId(1L)
                .channelId(2L)
                .channelTypeFlag((byte) 0)
                .build();

        sender.publish(task);

        verify(rabbitTemplate).convertAndSend(eq("dc3.e.alarm"), eq("dc3.r.notify.task.0"), eq(task));
    }

    @Test
    void usesUnknownRoutingKeyWhenChannelTypeIsMissing() {
        when(alarmExchange.getName()).thenReturn("dc3.e.alarm");
        NotifyTaskDTO task = NotifyTaskDTO.builder()
                .notifyHistoryId(1L)
                .channelId(2L)
                .build(); // channelTypeFlag missing

        sender.publish(task);

        verify(rabbitTemplate).convertAndSend(eq("dc3.e.alarm"), eq("dc3.r.notify.task.unknown"), eq(task));
    }

    @Test
    void refusesToPublishWhenHistoryIdIsMissing() {
        NotifyTaskDTO task = NotifyTaskDTO.builder().channelId(2L).build();
        sender.publish(task);
        verify(rabbitTemplate, never()).convertAndSend((String) any(), any(), (Object) any());
    }

}
