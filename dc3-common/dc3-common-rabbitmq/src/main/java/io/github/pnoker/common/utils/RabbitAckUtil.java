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

package io.github.pnoker.common.utils;

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * RabbitMQ manual acknowledgement helpers.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.9
 */
@Slf4j
public class RabbitAckUtil {

    public static void ack(Channel channel, long deliveryTag) throws IOException {
        channel.basicAck(deliveryTag, false);
    }

    public static void reject(Channel channel, long deliveryTag) {
        try {
            channel.basicReject(deliveryTag, false);
        } catch (IOException e) {
            log.error("RabbitMQ reject failed, deliveryTag: {}", deliveryTag, e);
        }
    }

    public static void nack(Channel channel, long deliveryTag, boolean requeue) {
        try {
            channel.basicNack(deliveryTag, false, requeue);
        } catch (IOException e) {
            log.error("RabbitMQ nack failed, deliveryTag: {}, requeue: {}", deliveryTag, requeue, e);
        }
    }

    private RabbitAckUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

}
