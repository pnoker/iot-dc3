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

package io.github.pnoker.common.data.rabbit;

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.data.buffer.PointValueIngestBuffer;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ receiver for point value ingestion events.
 *
 * <p>Every valid message is handed to {@link PointValueIngestBuffer}; ack when accepted,
 * nack-requeue when the buffer is full so RabbitMQ back-pressures instead of the center
 * OOM-ing. Uses the high-throughput container factory (wider prefetch / concurrency).
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueReceiver {

    private final PointValueIngestBuffer pointValueIngestBuffer;

    /**
     * Consume a point value message: validate, offer to the ingest buffer, ack on success or
     * nack-requeue when the buffer is full (back-pressure). Invalid messages are rejected.
     *
     * @param channel      the RabbitMQ channel for manual ack
     * @param message      the raw message carrying the delivery tag
     * @param pointValueBO the deserialized point value
     */
    @RabbitHandler
    @RabbitListener(queues = "#{pointValueQueue.name}",
            containerFactory = "highThroughputRabbitListenerContainerFactory")
    public void pointValueReceive(Channel channel, Message message, PointValueBO pointValueBO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            if (Objects.isNull(pointValueBO) || Objects.isNull(pointValueBO.getDeviceId())) {
                log.warn("Invalid point value, deviceId is null or pointValue is blank, deviceId={}",
                        Objects.isNull(pointValueBO) ? null : pointValueBO.getDeviceId());
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }
            if (pointValueIngestBuffer.offer(pointValueBO)) {
                RabbitAckUtil.ack(channel, deliveryTag);
            } else {
                log.warn("Point value ingest buffer full, nack-requeue to back-pressure, deviceId={}, pointId={}",
                        pointValueBO.getDeviceId(), pointValueBO.getPointId());
                RabbitAckUtil.nack(channel, deliveryTag, true);
            }
        } catch (Exception e) {
            log.error("Point value consume failed, deviceId={}, pointId={}, deliveryTag={}",
                    Objects.nonNull(pointValueBO) ? pointValueBO.getDeviceId() : null,
                    Objects.nonNull(pointValueBO) ? pointValueBO.getPointId() : null,
                    deliveryTag, e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }
}
