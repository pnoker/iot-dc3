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
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.data.entity.property.PointBatchProperties;
import io.github.pnoker.common.data.job.PointValueJob;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.utils.JsonUtil;
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
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueReceiver {

    private final PointBatchProperties pointBatchProperties;

    private final PointValueService pointValueService;

    /**
     * Consume a point value message: route it to the batch buffer when the receive speed
     * exceeds the threshold, otherwise save it directly. Manual ack on success, requeue
     * on failure.
     *
     * @param channel      the RabbitMQ channel for manual ack
     * @param message      the raw message carrying the delivery tag
     * @param pointValueBO the deserialized point value
     */
    @RabbitHandler
    @RabbitListener(queues = "#{pointValueQueue.name}")
    public void pointValueReceive(Channel channel, Message message, PointValueBO pointValueBO) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            if (Objects.isNull(pointValueBO) || Objects.isNull(pointValueBO.getDeviceId())) {
                log.warn("Invalid point value, deviceId is null or pointValue is blank, deviceId={}",
                        Objects.isNull(pointValueBO) ? null : pointValueBO.getDeviceId());
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }
            PointValueJob.recordPointValue();
            log.debug("Receive point value from: {}, {}", message.getMessageProperties().getReceivedRoutingKey(),
                    JsonUtil.toJsonString(pointValueBO));

            // Judge whether to process data in batch according to the data transmission
            // speed
            if (PointValueJob.getValueSpeed() < pointBatchProperties.getSpeed()) {
                // Save point value to local latest-value cache and repository storage
                pointValueService.save(pointValueBO);
            } else {
                // Save point value to schedule
                PointValueJob.addPointValues(pointValueBO);
            }
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Point value consume failed, deviceId={}, pointId={}, deliveryTag={}, routingKey={}",
                    Objects.nonNull(pointValueBO) ? pointValueBO.getDeviceId() : null,
                    Objects.nonNull(pointValueBO) ? pointValueBO.getPointId() : null,
                    deliveryTag, message.getMessageProperties().getReceivedRoutingKey(), e);
            RabbitAckUtil.nack(channel, deliveryTag, true);
        }
    }

}
