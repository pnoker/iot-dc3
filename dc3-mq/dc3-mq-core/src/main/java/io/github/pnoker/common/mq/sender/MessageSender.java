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

package io.github.pnoker.common.mq.sender;

import io.github.pnoker.common.mq.message.MqMessage;

import java.time.Duration;

/**
 * Business-facing send API. Implementations serialize the payload, stamp standard
 * headers, negotiate the delay capability and delegate to the active
 * {@code BrokerAdapter}.
 *
 * @author pnoker
 * @since 2026.8.19
 */
public interface MessageSender {

    /**
     * Fire-and-forget send. On confirming brokers this still publishes with publisher
     * confirms enabled; failures surface through the adapter's confirm logging.
     *
     * @param message the message to publish
     */
    void send(MqMessage message);

    /**
     * Async send with per-message confirmation. {@code confirmed=true} means the broker
     * accepted AND routed the message (where the broker can express routing). The
     * driver's durable outbox keys its delete/retry decision off this callback.
     *
     * @param message      the message to publish
     * @param confirmation invoked exactly once per send attempt
     */
    void sendAsync(MqMessage message, SendConfirmation confirmation);

    /**
     * Blocking send that waits up to {@code timeout} for proof of routing. Used by
     * low-volume state machines (command dispatch) that must not lose publishes.
     * Throws {@code MqPublishException} on nack, unroutable or timeout. On brokers
     * without publisher confirmation this degrades to a plain send.
     *
     * @param message the message to publish
     * @param timeout maximum time to wait for the broker's confirmation
     */
    void sendConfirmed(MqMessage message, Duration timeout);
}
