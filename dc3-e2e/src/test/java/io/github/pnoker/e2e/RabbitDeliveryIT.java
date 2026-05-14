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

package io.github.pnoker.e2e;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import io.github.pnoker.e2e.harness.BaseE2eIT;
import io.github.pnoker.e2e.harness.E2eStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Locks the RabbitMQ delivery contract DC3 relies on:
 * - publisher-confirm-mode=correlated guarantees the broker acks every routed message
 * - mandatory=true with a return listener fires on unroutable messages
 * - manual consumer ack drains a queue exactly once
 * <p>
 * Disabled by default; opt in with {@code DC3_E2E=true}.
 */
@EnabledIfEnvironmentVariable(named = "DC3_E2E", matches = "(?i)true|1|yes|on")
class RabbitDeliveryIT extends BaseE2eIT {

    private static Connection newConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(E2eStack.rabbitHost());
        factory.setPort(E2eStack.rabbitAmqpPort());
        factory.setUsername(E2eStack.rabbitUsername());
        factory.setPassword(E2eStack.rabbitPassword());
        return factory.newConnection();
    }

    @Test
    void confirmedPublishRoutesMessageToBoundQueue() throws Exception {
        try (Connection conn = newConnection();
             Channel channel = conn.createChannel()) {
            channel.confirmSelect();
            String exchange = "dc3.e2e.exchange." + UUID.randomUUID();
            String queue = "dc3.e2e.queue." + UUID.randomUUID();
            channel.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT, false, true, null);
            channel.queueDeclare(queue, false, false, true, null);
            channel.queueBind(queue, exchange, "rk");

            channel.basicPublish(exchange, "rk", null, "hello".getBytes(StandardCharsets.UTF_8));
            assertThat(channel.waitForConfirms(2_000L)).isTrue();

            AMQP.Queue.DeclareOk depth = channel.queueDeclarePassive(queue);
            assertThat(depth.getMessageCount()).isEqualTo(1);
        }
    }

    @Test
    void mandatoryReturnsUnroutableMessageThroughReturnListener() throws Exception {
        try (Connection conn = newConnection();
             Channel channel = conn.createChannel()) {
            CompletableFuture<String> returned = new CompletableFuture<>();
            channel.addReturnListener(ret -> returned.complete(new String(ret.getBody(), StandardCharsets.UTF_8)));

            String exchange = "dc3.e2e.exchange." + UUID.randomUUID();
            channel.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT, false, true, null);

            // No queue bound, mandatory=true -> broker sends a basic.return back.
            channel.basicPublish(exchange, "no-binding", true, false, null,
                    "lost".getBytes(StandardCharsets.UTF_8));

            assertThat(returned.get(2, TimeUnit.SECONDS)).isEqualTo("lost");
        }
    }

    @Test
    void manualAckDrainsQueueExactlyOnce() throws Exception {
        try (Connection conn = newConnection();
             Channel channel = conn.createChannel()) {
            String exchange = "dc3.e2e.exchange." + UUID.randomUUID();
            String queue = "dc3.e2e.queue." + UUID.randomUUID();
            channel.exchangeDeclare(exchange, BuiltinExchangeType.DIRECT, false, true, null);
            channel.queueDeclare(queue, false, false, true, null);
            channel.queueBind(queue, exchange, "k");

            channel.basicPublish(exchange, "k", null, "first".getBytes(StandardCharsets.UTF_8));
            channel.basicPublish(exchange, "k", null, "second".getBytes(StandardCharsets.UTF_8));

            await().atMost(Duration.ofSeconds(2))
                    .pollInterval(Duration.ofMillis(50))
                    .untilAsserted(() -> assertThat(channel.messageCount(queue)).isEqualTo(2));

            try (Channel delivery = conn.createChannel()) {
                GetResponse first = delivery.basicGet(queue, false);
                assertThat(first).isNotNull();
                assertThat(new String(first.getBody(), StandardCharsets.UTF_8)).isEqualTo("first");

                GetResponse second = delivery.basicGet(queue, false);
                assertThat(second).isNotNull();
                assertThat(new String(second.getBody(), StandardCharsets.UTF_8)).isEqualTo("second");
                delivery.basicAck(second.getEnvelope().getDeliveryTag(), false);
            }

            await().atMost(Duration.ofSeconds(2))
                    .pollInterval(Duration.ofMillis(50))
                    .untilAsserted(() -> assertThat(channel.messageCount(queue)).isEqualTo(1));

            GetResponse requeued = channel.basicGet(queue, false);
            assertThat(requeued).isNotNull();
            assertThat(new String(requeued.getBody(), StandardCharsets.UTF_8)).isEqualTo("first");
            channel.basicAck(requeued.getEnvelope().getDeliveryTag(), false);

            assertThat(channel.messageCount(queue)).isZero();
        }
    }

    @Test
    void waitForConfirmsWithoutPendingPublishesCompletesImmediately() throws Exception {
        try (Connection conn = newConnection();
             Channel channel = conn.createChannel()) {
            channel.confirmSelect();
            assertThat(channel.waitForConfirms(50L)).isTrue();
        }
    }

    @Test
    void exclusiveQueueRejectsConsumerOnDifferentConnection() throws Exception {
        try (Connection owner = newConnection();
             Connection other = newConnection();
             Channel first = owner.createChannel()) {
            String queue = "dc3.e2e.exclusive." + UUID.randomUUID();
            first.queueDeclare(queue, false, true, false, null);
            first.basicConsume(queue, true, "c1", (tag, delivery) -> {
            }, tag -> {
            });

            Channel second = other.createChannel();
            try {
                assertThatThrownBy(() -> second.basicConsume(queue, true, "c2", (tag, delivery) -> {
                }, tag -> {
                }))
                        .isInstanceOf(java.io.IOException.class)
                        .hasRootCauseInstanceOf(com.rabbitmq.client.ShutdownSignalException.class)
                        .hasStackTraceContaining("RESOURCE_LOCKED");
                assertThat(second.isOpen()).isFalse();
            } finally {
                if (second.isOpen()) {
                    second.close();
                }
            }
        }
    }

    @Test
    void connectionFactoryRefusesInvalidCredentialsFastPath() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(E2eStack.rabbitHost());
        factory.setPort(E2eStack.rabbitAmqpPort());
        factory.setUsername("dc3-e2e-not-a-user");
        factory.setPassword("nope");
        factory.setConnectionTimeout(1_500);
        assertThatThrownBy(factory::newConnection)
                .isInstanceOfAny(java.io.IOException.class, TimeoutException.class);
    }
}
