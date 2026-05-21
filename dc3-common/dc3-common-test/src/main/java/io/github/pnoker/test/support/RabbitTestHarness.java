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

package io.github.pnoker.test.support;

import lombok.RequiredArgsConstructor;
import org.awaitility.Awaitility;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Minimal helper around {@link RabbitTemplate} that captures the publish + assert
 * cycle most listener tests need: send a payload to an exchange/routing key, then
 * await an external signal driven by the consumer.
 */
@RequiredArgsConstructor
public final class RabbitTestHarness {

    private final RabbitTemplate rabbitTemplate;

    public void send(String exchange, String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }

    public Message receive(String queue, Duration timeout) {
        rabbitTemplate.setReceiveTimeout(timeout.toMillis());
        return rabbitTemplate.receive(queue);
    }

    /**
     * Block up to {@code timeout} for the supplied condition to evaluate {@code true}.
     * Backed by Awaitility so callers do not need to add a polling library directly.
     */
    public void awaitTrue(Duration timeout, Callable<Boolean> condition) {
        Awaitility.await().atMost(timeout).pollInterval(Duration.ofMillis(50)).until(condition);
    }
}
