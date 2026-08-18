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

import io.github.pnoker.common.constant.common.ExceptionConstant;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Publisher-confirm guard for low-volume messages whose state machine requires proof of routing. */
public final class RabbitPublishConfirm {

    private RabbitPublishConfirm() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Await routed.
     *
     * @param correlationData correlation data
     * @param timeout timeout
     */
    public static void awaitRouted(CorrelationData correlationData, Duration timeout) {
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.ack()) {
                throw new AmqpException("RabbitMQ publish NACK: " + confirm.reason());
            }
            if (correlationData.getReturned() != null) {
                throw new AmqpException("RabbitMQ publish was unroutable: "
                        + correlationData.getReturned().getReplyText());
            }
        } catch (AmqpException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Interrupted while waiting for RabbitMQ publisher confirm", e);
        } catch (Exception e) {
            throw new AmqpException("RabbitMQ publisher confirm timed out or failed", e);
        }
    }
}
