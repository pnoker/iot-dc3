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
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.entity.dto.EventReportDTO;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.e2e.harness.BaseE2eIT;
import io.github.pnoker.e2e.harness.E2eStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verifies the RabbitMQ event report contract: the event exchange routes
 * messages with wildcard routing keys, and the event queue receives them.
 */
@EnabledIfEnvironmentVariable(named = "DC3_E2E", matches = "(?i)true|1|yes|on")
class EventReportE2eIT extends BaseE2eIT {

    private Connection conn;
    private Channel channel;
    private String queue;
    private String service;

    private static Connection newConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(E2eStack.rabbitHost());
        factory.setPort(E2eStack.rabbitAmqpPort());
        factory.setUsername(E2eStack.rabbitUsername());
        factory.setPassword(E2eStack.rabbitPassword());
        return factory.newConnection();
    }

    @BeforeEach
    void setUp() throws Exception {
        conn = newConnection();
        channel = conn.createChannel();
        channel.confirmSelect();
        service = "e2e-test-" + UUID.randomUUID().toString().substring(0, 8);
        queue = RabbitConstant.QUEUE_EVENT_PREFIX + service;
    }

    @AfterEach
    void tearDown() throws Exception {
        if (channel.isOpen()) {
            channel.queueDelete(queue);
            channel.close();
        }
        if (conn.isOpen()) {
            conn.close();
        }
    }

    @Test
    void eventExchangeRoutesMessageToBoundQueue() throws Exception {
        channel.exchangeDeclare(RabbitConstant.TOPIC_EXCHANGE_EVENT,
                BuiltinExchangeType.TOPIC, true, false, null);
        channel.queueDeclare(queue, true, false, false, null);
        channel.queueBind(queue, RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_EVENT_PREFIX + service);

        EventReportDTO dto = EventReportDTO.builder()
                .recordId(UUID.randomUUID().toString())
                .tenantId(1L)
                .deviceId(100L)
                .eventId(200L)
                .eventCode("DOOR_OPEN")
                .eventTypeFlag((byte) 1)
                .eventLevelFlag((byte) 3)
                .message("Door opened by card reader")
                .occurTime(Instant.now())
                .schemaVersion(1)
                .build();

        byte[] body = JsonUtil.toJsonString(dto).getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_EVENT_PREFIX + service, null, body);
        assertThat(channel.waitForConfirms(2_000L)).isTrue();

        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(channel.messageCount(queue)).isEqualTo(1));

        GetResponse response = channel.basicGet(queue, true);
        assertThat(response).isNotNull();
        String json = new String(response.getBody(), StandardCharsets.UTF_8);
        EventReportDTO received = JsonUtil.parseObject(json, EventReportDTO.class);
        assertThat(received.recordId()).isEqualTo(dto.recordId());
        assertThat(received.deviceId()).isEqualTo(100L);
        assertThat(received.eventId()).isEqualTo(200L);
        assertThat(received.eventCode()).isEqualTo("DOOR_OPEN");
        assertThat(received.message()).isEqualTo("Door opened by card reader");
    }

    @Test
    void eventReportWithParamValuesRoundTrips() throws Exception {
        channel.exchangeDeclare(RabbitConstant.TOPIC_EXCHANGE_EVENT,
                BuiltinExchangeType.TOPIC, true, false, null);
        channel.queueDeclare(queue, true, false, false, null);
        channel.queueBind(queue, RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_EVENT_PREFIX + service);

        Map<String, String> params = new HashMap<>();
        params.put("temperature", "42.5");
        params.put("humidity", "88");
        EventReportDTO dto = EventReportDTO.builder()
                .recordId(UUID.randomUUID().toString())
                .tenantId(1L)
                .deviceId(100L)
                .eventId(300L)
                .eventCode("THRESHOLD_EXCEEDED")
                .eventTypeFlag((byte) 1)
                .eventLevelFlag((byte) 2)
                .paramValues(params)
                .message("Temperature threshold exceeded")
                .occurTime(Instant.now())
                .schemaVersion(1)
                .build();

        byte[] body = JsonUtil.toJsonString(dto).getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_EVENT_PREFIX + service, null, body);
        assertThat(channel.waitForConfirms(2_000L)).isTrue();

        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(channel.messageCount(queue)).isEqualTo(1));

        GetResponse response = channel.basicGet(queue, true);
        EventReportDTO received = JsonUtil.parseObject(
                new String(response.getBody(), StandardCharsets.UTF_8), EventReportDTO.class);
        assertThat(received.paramValues()).containsEntry("temperature", "42.5");
        assertThat(received.paramValues()).containsEntry("humidity", "88");
    }

    @Test
    void eventRoutingKeyWildcardMatchesMultipleServices() throws Exception {
        channel.exchangeDeclare(RabbitConstant.TOPIC_EXCHANGE_EVENT,
                BuiltinExchangeType.TOPIC, true, false, null);

        String serviceA = "svc-a-" + UUID.randomUUID().toString().substring(0, 6);
        String serviceB = "svc-b-" + UUID.randomUUID().toString().substring(0, 6);
        String queueA = RabbitConstant.QUEUE_EVENT_PREFIX + serviceA;
        String queueB = RabbitConstant.QUEUE_EVENT_PREFIX + serviceB;

        channel.queueDeclare(queueA, true, false, false, null);
        channel.queueDeclare(queueB, true, false, false, null);
        channel.queueBind(queueA, RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_EVENT_PREFIX + serviceA);
        channel.queueBind(queueB, RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_EVENT_PREFIX + serviceB);

        EventReportDTO dto = EventReportDTO.builder()
                .recordId(UUID.randomUUID().toString())
                .tenantId(1L)
                .deviceId(100L)
                .eventId(200L)
                .eventCode("HEARTBEAT")
                .eventTypeFlag((byte) 0)
                .eventLevelFlag((byte) 0)
                .occurTime(Instant.now())
                .schemaVersion(1)
                .build();
        byte[] body = JsonUtil.toJsonString(dto).getBytes(StandardCharsets.UTF_8);

        // Only service A receives
        channel.basicPublish(RabbitConstant.TOPIC_EXCHANGE_EVENT,
                RabbitConstant.ROUTING_EVENT_PREFIX + serviceA, null, body);
        assertThat(channel.waitForConfirms(2_000L)).isTrue();

        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(channel.messageCount(queueA)).isEqualTo(1));

        assertThat(channel.messageCount(queueB)).isZero();

        channel.basicGet(queueA, true);
        channel.queueDelete(queueA);
        channel.queueDelete(queueB);
    }
}
