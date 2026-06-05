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

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.dto.NotifyTaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

/**
 * Publishes {@link NotifyTaskDTO} payloads to the alarm exchange so the
 * NotifyWorker can dispatch the actual channel send asynchronously. Routing
 * key includes a channel-type segment so consumers can shape topology later
 * (e.g. dedicated workers per channel) without re-writing the producer.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyTaskSender {

    private final RabbitTemplate rabbitTemplate;

    private final TopicExchange alarmExchange;

    public void publish(NotifyTaskDTO task) {
        if (Objects.isNull(task) || Objects.isNull(task.getNotifyHistoryId())) {
            log.warn("Refusing to publish notify task without a history id: {}", task);
            return;
        }
        String channelType = Objects.nonNull(task.getChannelTypeFlag())
                ? task.getChannelTypeFlag().toString()
                : DataConstant.STATUS_UNKNOWN;
        String routingKey = (RabbitConstant.ROUTING_NOTIFY_TASK_PREFIX + channelType).toLowerCase(Locale.ROOT);
        rabbitTemplate.convertAndSend(alarmExchange.getName(), routingKey, task);
    }

}
