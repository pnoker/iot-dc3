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
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RabbitMQ receiver for point value ingestion events.
 *
 * <p>The listener receives broker-created batches, validates the complete wire contract,
 * persists history and latest projections in one transaction, and acknowledges the batch
 * only after that transaction commits.
 *
 * @author pnoker
 * @version 2026.7.8
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueReceiver {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final PointValueService pointValueService;

    /**
     * Consume and durably persist one broker batch.
     *
     * @param channel      the RabbitMQ channel for manual ack
     * @param messages raw messages in broker delivery order
     */
    @RabbitListener(queues = "#{pointValueQueue.name}",
            containerFactory = "pointValueRabbitListenerContainerFactory")
    public void pointValueReceive(List<Message> messages, Channel channel) throws IOException {
        if (messages.isEmpty()) {
            return;
        }

        List<PointValueBO> values = new ArrayList<>(messages.size());
        for (Message message : messages) {
            PointValueBO value;
            try {
                value = JsonUtil.parseObject(message.getBody(), PointValueBO.class);
            } catch (Exception e) {
                throw poison(message, "Point-value payload is not valid JSON", e);
            }
            if (!valid(value)) {
                throw poison(message, "Point-value payload violates schema version 1", null);
            }
            values.add(value);
        }

        pointValueService.save(values);

        long lastDeliveryTag = messages.getLast().getMessageProperties().getDeliveryTag();
        channel.basicAck(lastDeliveryTag, true);
        log.debug("Persisted and acknowledged point-value batch, size={}, lastDeliveryTag={}",
                values.size(), lastDeliveryTag);
    }

    private boolean valid(PointValueBO value) {
        return Objects.nonNull(value)
                && Objects.equals(value.getSchemaVersion(), SUPPORTED_SCHEMA_VERSION)
                && Objects.nonNull(value.getMessageId()) && !value.getMessageId().isBlank()
                && Objects.nonNull(value.getDriverNode()) && !value.getDriverNode().isBlank()
                && positive(value.getSequence())
                && positive(value.getFencingToken())
                && positive(value.getTenantId())
                && positive(value.getDriverId())
                && positive(value.getDeviceId())
                && positive(value.getPointId())
                && Objects.nonNull(value.getRawValue())
                && Objects.nonNull(value.getCalValue())
                && Objects.nonNull(value.getCreateTime());
    }

    private boolean positive(Long value) {
        return Objects.nonNull(value) && value > 0;
    }

    private AmqpRejectAndDontRequeueException poison(Message message, String reason, Exception cause) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.error("Reject poison point-value batch, reason={}, deliveryTag={}", reason, deliveryTag, cause);
        return new AmqpRejectAndDontRequeueException(reason, true, cause);
    }
}
