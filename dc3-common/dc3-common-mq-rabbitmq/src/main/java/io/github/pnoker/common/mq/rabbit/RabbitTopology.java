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

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

/**
 * Declares the physical RabbitMQ topology (exchanges, queues, bindings) with arguments
 * byte-for-byte identical to the pre-port {@code ExchangeConfig}/{@code DataTopicConfig}
 * layout: same names, same TTLs, same dead-letter wiring, same binding arguments. All
 * declarations are idempotent, so a rolling deployment across mixed versions converges
 * on the same topology.
 *
 * @author pnoker
 * @since 2026.8.19
 */
public final class RabbitTopology {

    /**
     * Binding argument carried over from the pre-port layout; kept for wire identity.
     */
    private static final String BINDING_AUTO_DELETE = "x-auto-delete";

    private RabbitTopology() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Declare every platform-shared exchange, queue and binding. Driver-side per-instance
     * queues are declared on subscribe (they embed the driver client id).
     *
     * @param admin the rabbit admin to declare through
     */
    public static void declareSharedTopology(RabbitAdmin admin) {
        TopicExchange state = declareExchange(admin, RabbitNames.EXCHANGE_STATE);
        TopicExchange alarm = declareExchange(admin, RabbitNames.EXCHANGE_ALARM);
        TopicExchange metadata = declareExchange(admin, RabbitNames.EXCHANGE_METADATA);
        TopicExchange pointCommand = declareExchange(admin, RabbitNames.EXCHANGE_POINT_COMMAND);
        TopicExchange value = declareExchange(admin, RabbitNames.EXCHANGE_VALUE);
        TopicExchange timeoutDelay = declareExchange(admin, RabbitNames.EXCHANGE_STATE_TIMEOUT_DELAY);
        TopicExchange timeoutCheck = declareExchange(admin, RabbitNames.EXCHANGE_STATE_TIMEOUT_CHECK);
        TopicExchange command = declareExchange(admin, RabbitNames.EXCHANGE_COMMAND);
        TopicExchange commandResult = declareExchange(admin, RabbitNames.EXCHANGE_COMMAND_RESULT);
        TopicExchange commandDead = declareExchange(admin, RabbitNames.EXCHANGE_COMMAND_DEAD);
        TopicExchange event = declareExchange(admin, RabbitNames.EXCHANGE_EVENT);
        TopicExchange pointValueDead = declareExchange(admin, RabbitNames.EXCHANGE_POINT_VALUE_DEAD);
        TopicExchange pointCommandDead = declareExchange(admin, RabbitNames.EXCHANGE_POINT_COMMAND_DEAD);
        declareExchange(admin, RabbitNames.EXCHANGE_POINT_COMMAND_RESULT);

        // state / alarm queues, 30 s TTL (these bindings carry the x-auto-delete argument)
        bind(admin, QueueBuilder.durable(RabbitNames.QUEUE_DRIVER_STATE).ttl(30_000).build(),
                state, "dc3.r.state.driver.*");
        bind(admin, QueueBuilder.durable(RabbitNames.QUEUE_DEVICE_STATE).ttl(30_000).build(),
                state, "dc3.r.state.device.*");
        bind(admin, QueueBuilder.durable(RabbitNames.QUEUE_DRIVER_ALARM).ttl(30_000).build(),
                alarm, "dc3.r.alarm.driver.*");
        bind(admin, QueueBuilder.durable(RabbitNames.QUEUE_DEVICE_ALARM).ttl(30_000).build(),
                alarm, "dc3.r.alarm.device.*");

        // point value: 7 d TTL then dead-letter to the quarantine exchange
        bind(admin, QueueBuilder.durable(RabbitNames.QUEUE_POINT_VALUE)
                        .ttl(604_800_000)
                        .deadLetterExchange(RabbitNames.EXCHANGE_POINT_VALUE_DEAD)
                        .deadLetterRoutingKey("#")
                        .build(),
                value, "dc3.r.value.point.*");
        bindPlain(admin, QueueBuilder.durable(RabbitNames.QUEUE_POINT_VALUE_DEAD).build(), pointValueDead, "#");

        // notify task: 24 h TTL guard against runaway outbound backlog
        bind(admin, QueueBuilder.durable(RabbitNames.QUEUE_NOTIFY_TASK).ttl(86_400_000).build(),
                alarm, "dc3.r.notify.task.*");

        // driver timeout delay chain (45 s TTL + DLX)
        bindPlain(admin, QueueBuilder.durable(RabbitNames.QUEUE_DRIVER_TIMEOUT_DELAY)
                        .ttl(45_000)
                        .deadLetterExchange(RabbitNames.EXCHANGE_STATE_TIMEOUT_CHECK)
                        .deadLetterRoutingKey(RabbitNames.ROUTING_DRIVER_TIMEOUT_CHECK)
                        .build(),
                timeoutDelay, RabbitNames.ROUTING_DRIVER_TIMEOUT_DELAY);
        bindPlain(admin, QueueBuilder.durable(RabbitNames.QUEUE_DRIVER_TIMEOUT_CHECK).build(),
                timeoutCheck, RabbitNames.ROUTING_DRIVER_TIMEOUT_CHECK);

        // device scan tick chain (10 s TTL + DLX)
        bindPlain(admin, QueueBuilder.durable(RabbitNames.QUEUE_DEVICE_SCAN_TICK)
                        .ttl(10_000)
                        .deadLetterExchange(RabbitNames.EXCHANGE_STATE_TIMEOUT_CHECK)
                        .deadLetterRoutingKey(RabbitNames.ROUTING_DEVICE_SCAN)
                        .build(),
                timeoutDelay, RabbitNames.ROUTING_DEVICE_SCAN_TICK);
        bindPlain(admin, QueueBuilder.durable(RabbitNames.QUEUE_DEVICE_SCAN).build(),
                timeoutCheck, RabbitNames.ROUTING_DEVICE_SCAN);

        // dead letters and results (plain bindings, no binding argument)
        bindPlain(admin, QueueBuilder.durable(RabbitNames.QUEUE_POINT_COMMAND_DEAD).build(), pointCommandDead, "#");
        bindPlain(admin, QueueBuilder.durable(RabbitNames.QUEUE_COMMAND_DEAD).build(), commandDead, "#");
        bindPlain(admin, QueueBuilder.durable(RabbitNames.QUEUE_POINT_COMMAND_RESULT).ttl(60_000).build(),
                declareExchange(admin, RabbitNames.EXCHANGE_POINT_COMMAND_RESULT),
                "dc3.r.point_command_result.*");
        bindPlain(admin, QueueBuilder.durable(RabbitNames.QUEUE_COMMAND_RESULT).ttl(60_000).build(),
                commandResult, RabbitNames.ROUTING_COMMAND_RESULT_PREFIX + "*");
        bindPlain(admin, QueueBuilder.durable(RabbitNames.QUEUE_EVENT_REPORT).ttl(60_000).build(),
                event, RabbitNames.ROUTING_EVENT_PREFIX + "*");

        // metadata / point command / command exchanges are also consumed by per-instance
        // driver queues declared on subscribe; the exchanges above are already declared.
        assert metadata != null && pointCommand != null && command != null;
    }

    /**
     * Declare the driver-side metadata broadcast queue (auto-delete, 30 s TTL).
     *
     * @param admin  rabbit admin
     * @param client driver client id
     * @param routingKey exact routing key (service name)
     */
    public static void declareMetadataQueue(RabbitAdmin admin, String client, String routingKey) {
        Queue queue = QueueBuilder.durable(RabbitNames.QUEUE_DRIVER_METADATA_PREFIX + client)
                .autoDelete()
                .ttl(30_000)
                .build();
        Binding binding = BindingBuilder.bind(queue)
                .to(new TopicExchange(RabbitNames.EXCHANGE_METADATA))
                .with(routingKey);
        binding.addArgument(BINDING_AUTO_DELETE, false);
        admin.declareQueue(queue);
        admin.declareBinding(binding);
    }

    /**
     * Declare a driver-side command queue (30 s TTL, lease-coupled expiry, dead-letter
     * to the matching dead exchange).
     *
     * @param admin            rabbit admin
     * @param queueName        full queue name (prefix + client id)
     * @param exchangeName     source exchange
     * @param deadExchangeName dead-letter exchange
     * @param routingKey       exact routing key (service.node)
     * @param expiresMillis    lease-coupled queue expiry (x-expires)
     */
    public static void declareDriverCommandQueue(RabbitAdmin admin, String queueName, String exchangeName,
                                                 String deadExchangeName, String routingKey, int expiresMillis) {
        Queue queue = QueueBuilder.durable(queueName)
                .ttl(30_000)
                .expires(expiresMillis)
                .deadLetterExchange(deadExchangeName)
                .deadLetterRoutingKey("#")
                .build();
        Binding binding = BindingBuilder.bind(queue)
                .to(new TopicExchange(exchangeName))
                .with(routingKey);
        binding.addArgument(BINDING_AUTO_DELETE, false);
        admin.declareQueue(queue);
        admin.declareBinding(binding);
    }

    private static TopicExchange declareExchange(RabbitAdmin admin, String name) {
        TopicExchange exchange = new TopicExchange(name, true, false);
        admin.declareExchange(exchange);
        return exchange;
    }

    private static void bind(RabbitAdmin admin, Queue queue, TopicExchange exchange, String routingKey) {
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        binding.addArgument(BINDING_AUTO_DELETE, false);
        admin.declareQueue(queue);
        admin.declareBinding(binding);
    }

    private static void bindPlain(RabbitAdmin admin, Queue queue, TopicExchange exchange, String routingKey) {
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(routingKey));
    }
}
