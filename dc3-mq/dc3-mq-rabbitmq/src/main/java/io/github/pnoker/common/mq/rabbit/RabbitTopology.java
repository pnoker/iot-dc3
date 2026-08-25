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

import java.util.List;
import java.util.Objects;

import java.util.List;

/**
 * Declares the physical RabbitMQ topology (exchanges, queues, bindings) with arguments
 * byte-for-byte identical to the pre-port {@code ExchangeConfig}/{@code DataTopicConfig}
 * layout: same names, same TTLs, same dead-letter wiring, same binding arguments. All
 * declarations are idempotent, so a rolling deployment across mixed versions converges
 * on the same topology.
 *
 * <p>Platform-shared queues have a descriptor table; a subscription carrying a non-blank
 * {@code group} gets a group-suffixed copy of the same queue (same arguments, same
 * binding). The blank-group names are exactly the pre-port ones, so production
 * deployments are unchanged — the suffix exists for named consumer groups and for
 * contract-test isolation.
 *
 * @author pnoker
 * @since 2026.8.19
 */
public final class RabbitTopology {

    /**
     * Binding argument carried over from the pre-port layout; kept for wire identity.
     */
    private static final String BINDING_AUTO_DELETE = "x-auto-delete";

    /**
     * Descriptor of a platform-shared queue.
     *
     * @param queueName       queue name
     * @param exchangeName    source exchange
     * @param routingKey      binding routing key (pattern)
     * @param ttlMillis       per-queue message TTL, 0 = none
     * @param deadExchange    dead-letter exchange, null = none
     * @param deadRouting     dead-letter routing key
     * @param bindingArgument whether the binding carries the x-auto-delete argument
     */
    private record SharedQueue(String queueName, String exchangeName, String routingKey, int ttlMillis,
                               String deadExchange, String deadRouting, boolean bindingArgument) {
    }

    private static final List<SharedQueue> SHARED_QUEUES = List.of(
            new SharedQueue(RabbitNames.QUEUE_DRIVER_STATE, RabbitNames.EXCHANGE_STATE,
                    "dc3.r.state.driver.*", 30_000, null, null, true),
            new SharedQueue(RabbitNames.QUEUE_DEVICE_STATE, RabbitNames.EXCHANGE_STATE,
                    "dc3.r.state.device.*", 30_000, null, null, true),
            new SharedQueue(RabbitNames.QUEUE_DRIVER_ALARM, RabbitNames.EXCHANGE_ALARM,
                    "dc3.r.alarm.driver.*", 30_000, null, null, true),
            new SharedQueue(RabbitNames.QUEUE_DEVICE_ALARM, RabbitNames.EXCHANGE_ALARM,
                    "dc3.r.alarm.device.*", 30_000, null, null, true),
            new SharedQueue(RabbitNames.QUEUE_POINT_VALUE, RabbitNames.EXCHANGE_VALUE,
                    "dc3.r.value.point.*", 604_800_000, RabbitNames.EXCHANGE_POINT_VALUE_DEAD, "#", true),
            new SharedQueue(RabbitNames.QUEUE_POINT_VALUE_DEAD, RabbitNames.EXCHANGE_POINT_VALUE_DEAD,
                    "#", 0, null, null, false),
            new SharedQueue(RabbitNames.QUEUE_NOTIFY_TASK, RabbitNames.EXCHANGE_ALARM,
                    "dc3.r.notify.task.*", 86_400_000, null, null, true),
            new SharedQueue(RabbitNames.QUEUE_DRIVER_TIMEOUT_DELAY, RabbitNames.EXCHANGE_STATE_TIMEOUT_DELAY,
                    RabbitNames.ROUTING_DRIVER_TIMEOUT_DELAY, 45_000,
                    RabbitNames.EXCHANGE_STATE_TIMEOUT_CHECK, RabbitNames.ROUTING_DRIVER_TIMEOUT_CHECK, false),
            new SharedQueue(RabbitNames.QUEUE_DRIVER_TIMEOUT_CHECK, RabbitNames.EXCHANGE_STATE_TIMEOUT_CHECK,
                    RabbitNames.ROUTING_DRIVER_TIMEOUT_CHECK, 0, null, null, false),
            new SharedQueue(RabbitNames.QUEUE_DEVICE_SCAN_TICK, RabbitNames.EXCHANGE_STATE_TIMEOUT_DELAY,
                    RabbitNames.ROUTING_DEVICE_SCAN_TICK, 10_000,
                    RabbitNames.EXCHANGE_STATE_TIMEOUT_CHECK, RabbitNames.ROUTING_DEVICE_SCAN, false),
            new SharedQueue(RabbitNames.QUEUE_DEVICE_SCAN, RabbitNames.EXCHANGE_STATE_TIMEOUT_CHECK,
                    RabbitNames.ROUTING_DEVICE_SCAN, 0, null, null, false),
            new SharedQueue(RabbitNames.QUEUE_POINT_COMMAND_DEAD, RabbitNames.EXCHANGE_POINT_COMMAND_DEAD,
                    "#", 0, null, null, false),
            new SharedQueue(RabbitNames.QUEUE_COMMAND_DEAD, RabbitNames.EXCHANGE_COMMAND_DEAD,
                    "#", 0, null, null, false),
            new SharedQueue(RabbitNames.QUEUE_POINT_COMMAND_RESULT, RabbitNames.EXCHANGE_POINT_COMMAND_RESULT,
                    "dc3.r.point_command_result.*", 60_000, null, null, false),
            new SharedQueue(RabbitNames.QUEUE_COMMAND_RESULT, RabbitNames.EXCHANGE_COMMAND_RESULT,
                    RabbitNames.ROUTING_COMMAND_RESULT_PREFIX + "*", 60_000, null, null, false),
            new SharedQueue(RabbitNames.QUEUE_EVENT_REPORT, RabbitNames.EXCHANGE_EVENT,
                    RabbitNames.ROUTING_EVENT_PREFIX + "*", 60_000, null, null, false));

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
        for (String exchange : new String[]{
                RabbitNames.EXCHANGE_STATE, RabbitNames.EXCHANGE_ALARM, RabbitNames.EXCHANGE_METADATA,
                RabbitNames.EXCHANGE_POINT_COMMAND, RabbitNames.EXCHANGE_VALUE,
                RabbitNames.EXCHANGE_STATE_TIMEOUT_DELAY, RabbitNames.EXCHANGE_STATE_TIMEOUT_CHECK,
                RabbitNames.EXCHANGE_COMMAND, RabbitNames.EXCHANGE_COMMAND_RESULT,
                RabbitNames.EXCHANGE_COMMAND_DEAD, RabbitNames.EXCHANGE_EVENT,
                RabbitNames.EXCHANGE_POINT_VALUE_DEAD, RabbitNames.EXCHANGE_POINT_COMMAND_DEAD,
                RabbitNames.EXCHANGE_POINT_COMMAND_RESULT}) {
            admin.declareExchange(new TopicExchange(exchange, true, false));
        }
        SHARED_QUEUES.forEach(def -> declare(admin, def, def.queueName()));
    }

    /**
     * Declare a group-suffixed copy of a platform-shared queue (same arguments, same
     * binding) for named consumer groups; used by the contract suite for isolation.
     *
     * @param admin     rabbit admin
     * @param baseQueue base queue name from {@link RabbitNames}
     * @param group     consumer group suffix
     * @return the suffixed queue name
     */
    public static String declareGroupedQueue(RabbitAdmin admin, String baseQueue, String group) {
        SHARED_QUEUES.stream()
                .filter(def -> def.queueName().equals(baseQueue))
                .findFirst()
                .ifPresent(def -> declare(admin, def, baseQueue + "." + group));
        return baseQueue + "." + group;
    }

    /**
     * Declare the driver-side metadata broadcast queue (auto-delete, 30 s TTL).
     *
     * @param admin      rabbit admin
     * @param client     driver client id
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

    private static void declare(RabbitAdmin admin, SharedQueue def, String queueName) {
        QueueBuilder builder = QueueBuilder.durable(queueName);
        if (def.ttlMillis() > 0) {
            builder.ttl(def.ttlMillis());
        }
        if (Objects.nonNull(def.deadExchange())) {
            builder.deadLetterExchange(def.deadExchange()).deadLetterRoutingKey(def.deadRouting());
        }
        Queue queue = builder.build();
        Binding binding = BindingBuilder.bind(queue)
                .to(new TopicExchange(def.exchangeName()))
                .with(def.routingKey());
        if (def.bindingArgument()) {
            binding.addArgument(BINDING_AUTO_DELETE, false);
        }
        admin.declareQueue(queue);
        admin.declareBinding(binding);
    }
}
