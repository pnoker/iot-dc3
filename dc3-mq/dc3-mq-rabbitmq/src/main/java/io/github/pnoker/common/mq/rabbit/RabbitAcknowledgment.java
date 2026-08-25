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

package io.github.pnoker.common.mq.rabbit;

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import org.springframework.amqp.AmqpException;

import java.io.IOException;

/**
 * Port {@link Acknowledgment} backed by a RabbitMQ channel delivery tag. The batch
 * variant acknowledges up to the last delivery tag with {@code multiple=true}, which is
 * how the pre-port point-value consumer committed a whole broker batch at once.
 *
 * @author pnoker
 * @since 2026.8.19
 */
public final class RabbitAcknowledgment implements Acknowledgment {

    private final Channel channel;
    private final long deliveryTag;
    private final boolean multiple;

    public RabbitAcknowledgment(Channel channel, long deliveryTag, boolean multiple) {
        this.channel = channel;
        this.deliveryTag = deliveryTag;
        this.multiple = multiple;
    }

    /**
     * Ack exactly one delivery ({@code multiple=false}).
     */
    public static RabbitAcknowledgment single(Channel channel, long deliveryTag) {
        return new RabbitAcknowledgment(channel, deliveryTag, false);
    }

    /**
     * Ack everything up to the tag ({@code multiple=true}) — the broker-batch commit path.
     */
    public static RabbitAcknowledgment batch(Channel channel, long lastDeliveryTag) {
        return new RabbitAcknowledgment(channel, lastDeliveryTag, true);
    }

    @Override
    public void ack() {
        try {
            channel.basicAck(deliveryTag, multiple);
        } catch (IOException e) {
            throw new AmqpException("Failed to ack delivery " + deliveryTag, e);
        }
    }

    @Override
    public void reject(boolean requeue) {
        try {
            channel.basicNack(deliveryTag, multiple, requeue);
        } catch (IOException e) {
            throw new AmqpException("Failed to reject delivery " + deliveryTag, e);
        }
    }
}
