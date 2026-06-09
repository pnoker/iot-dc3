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

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.entity.dto.CommandCallDTO;
import io.github.pnoker.common.entity.dto.CommandCallResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
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
 * Verifies the RabbitMQ custom command contract: dispatch to driver-specific
 * queue, DLX on TTL expiry, and result delivery.
 */
@EnabledIfEnvironmentVariable(named = "DC3_E2E", matches = "(?i)true|1|yes|on")
class CommandCallE2eIT extends BaseE2eIT {

    private Connection conn;
    private Channel channel;
    private String driverService;
    private String commandQueue;
    private String deadQueue;

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
        driverService = "e2e-driver-" + UUID.randomUUID().toString().substring(0, 8);
        commandQueue = RabbitConstant.QUEUE_COMMAND_PREFIX + driverService;
        deadQueue = RabbitConstant.QUEUE_COMMAND_DEAD + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (channel.isOpen()) {
            try {
                channel.queueDelete(commandQueue);
            } catch (Exception ignored) {
            }
            try {
                channel.queueDelete(deadQueue);
            } catch (Exception ignored) {
            }
            channel.close();
        }
        if (conn.isOpen()) {
            conn.close();
        }
    }

    @Test
    void commandMessageRoutesToDriverSpecificQueue() throws Exception {
        channel.exchangeDeclare(RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                BuiltinExchangeType.TOPIC, true, false, null);
        channel.queueDeclare(commandQueue, true, false, false, null);
        channel.queueBind(commandQueue, RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_COMMAND_PREFIX + driverService);

        CommandCallDTO dto = CommandCallDTO.builder()
                .recordId(UUID.randomUUID().toString())
                .tenantId(1L)
                .deviceId(100L)
                .commandId(200L)
                .commandCode("RESTART")
                .paramValues(Map.of("mode", "graceful"))
                .expireAt(Instant.now().plusSeconds(30))
                .schemaVersion(1)
                .build();

        byte[] body = JsonUtil.toJsonString(dto).getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_COMMAND_PREFIX + driverService, null, body);
        assertThat(channel.waitForConfirms(2_000L)).isTrue();

        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(channel.messageCount(commandQueue)).isEqualTo(1));

        GetResponse response = channel.basicGet(commandQueue, true);
        assertThat(response).isNotNull();
        String json = new String(response.getBody(), StandardCharsets.UTF_8);
        CommandCallDTO received = JsonUtil.parseObject(json, CommandCallDTO.class);
        assertThat(received.recordId()).isEqualTo(dto.recordId());
        assertThat(received.deviceId()).isEqualTo(100L);
        assertThat(received.commandId()).isEqualTo(200L);
        assertThat(received.commandCode()).isEqualTo("RESTART");
        assertThat(received.paramValues()).containsEntry("mode", "graceful");
    }

    @Test
    void expiredCommandRoutesToDeadLetterQueue() throws Exception {
        channel.exchangeDeclare(RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                BuiltinExchangeType.TOPIC, true, false, null);
        channel.exchangeDeclare(RabbitConstant.TOPIC_EXCHANGE_COMMAND_DEAD,
                BuiltinExchangeType.TOPIC, true, false, null);

        Map<String, Object> args = new HashMap<>();
        args.put(RabbitConstant.MESSAGE_TTL, 500);
        args.put("x-dead-letter-exchange", RabbitConstant.TOPIC_EXCHANGE_COMMAND_DEAD);
        args.put("x-dead-letter-routing-key", "#");

        channel.queueDeclare(commandQueue, true, false, false, args);
        channel.queueBind(commandQueue, RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_COMMAND_PREFIX + driverService);

        channel.queueDeclare(deadQueue, true, false, false, null);
        channel.queueBind(deadQueue, RabbitConstant.TOPIC_EXCHANGE_COMMAND_DEAD, "#");

        CommandCallDTO dto = CommandCallDTO.builder()
                .recordId(UUID.randomUUID().toString())
                .tenantId(1L)
                .deviceId(100L)
                .commandId(200L)
                .commandCode("RESTART")
                .expireAt(Instant.now().plusSeconds(1))
                .schemaVersion(1)
                .build();

        byte[] body = JsonUtil.toJsonString(dto).getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_COMMAND_PREFIX + driverService, null, body);
        assertThat(channel.waitForConfirms(2_000L)).isTrue();

        // Wait for TTL to expire and message to be dead-lettered
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(channel.messageCount(deadQueue)).isEqualTo(1));

        assertThat(channel.messageCount(commandQueue)).isZero();

        GetResponse dead = channel.basicGet(deadQueue, true);
        assertThat(dead).isNotNull();
        String json = new String(dead.getBody(), StandardCharsets.UTF_8);
        CommandCallDTO deadMessage = JsonUtil.parseObject(json, CommandCallDTO.class);
        assertThat(deadMessage.recordId()).isEqualTo(dto.recordId());
    }

    @Test
    void commandResultRoutesToResultQueue() throws Exception {
        channel.exchangeDeclare(RabbitConstant.TOPIC_EXCHANGE_COMMAND_RESULT,
                BuiltinExchangeType.TOPIC, true, false, null);

        String resultQueue = RabbitConstant.QUEUE_COMMAND_RESULT;
        channel.queueDeclare(resultQueue, true, false, false, null);
        channel.queueBind(resultQueue, RabbitConstant.TOPIC_EXCHANGE_COMMAND_RESULT,
                RabbitConstant.ROUTING_COMMAND_RESULT + "." + driverService);

        CommandCallResultDTO result = CommandCallResultDTO.builder()
                .recordId(UUID.randomUUID().toString())
                .tenantId(1L)
                .status(PointCommandStatusEnum.SUCCESS)
                .resultValues(Map.of("uptime", "3600"))
                .finishedAt(Instant.now())
                .schemaVersion(1)
                .build();

        byte[] body = JsonUtil.toJsonString(result).getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(RabbitConstant.TOPIC_EXCHANGE_COMMAND_RESULT,
                RabbitConstant.ROUTING_COMMAND_RESULT + "." + driverService, null, body);
        assertThat(channel.waitForConfirms(2_000L)).isTrue();

        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(channel.messageCount(resultQueue)).isEqualTo(1));

        GetResponse response = channel.basicGet(resultQueue, true);
        assertThat(response).isNotNull();
        String json = new String(response.getBody(), StandardCharsets.UTF_8);
        CommandCallResultDTO received = JsonUtil.parseObject(json, CommandCallResultDTO.class);
        assertThat(received.recordId()).isEqualTo(result.recordId());
        assertThat(received.status()).isEqualTo(PointCommandStatusEnum.SUCCESS);
        assertThat(received.resultValues()).containsEntry("uptime", "3600");

        channel.queueDelete(resultQueue);
    }

    @Test
    void multipleDriversReceiveOnlyTheirOwnCommands() throws Exception {
        channel.exchangeDeclare(RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                BuiltinExchangeType.TOPIC, true, false, null);

        String driverA = "e2e-da-" + UUID.randomUUID().toString().substring(0, 6);
        String driverB = "e2e-db-" + UUID.randomUUID().toString().substring(0, 6);
        String qA = RabbitConstant.QUEUE_COMMAND_PREFIX + driverA;
        String qB = RabbitConstant.QUEUE_COMMAND_PREFIX + driverB;

        channel.queueDeclare(qA, true, false, false, null);
        channel.queueDeclare(qB, true, false, false, null);
        channel.queueBind(qA, RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_COMMAND_PREFIX + driverA);
        channel.queueBind(qB, RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_COMMAND_PREFIX + driverB);

        CommandCallDTO dto = CommandCallDTO.builder()
                .recordId(UUID.randomUUID().toString())
                .tenantId(1L)
                .deviceId(100L)
                .commandId(200L)
                .commandCode("RESTART")
                .expireAt(Instant.now().plusSeconds(30))
                .schemaVersion(1)
                .build();
        byte[] body = JsonUtil.toJsonString(dto).getBytes(StandardCharsets.UTF_8);

        // Route only to driver A
        channel.basicPublish(RabbitConstant.TOPIC_EXCHANGE_COMMAND,
                RabbitConstant.ROUTING_COMMAND_PREFIX + driverA, null, body);
        assertThat(channel.waitForConfirms(2_000L)).isTrue();

        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(channel.messageCount(qA)).isEqualTo(1));

        assertThat(channel.messageCount(qB)).isZero();

        channel.queueDelete(qA);
        channel.queueDelete(qB);
    }
}
