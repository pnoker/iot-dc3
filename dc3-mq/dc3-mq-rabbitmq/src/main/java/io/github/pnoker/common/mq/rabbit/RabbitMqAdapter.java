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
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.adapter.BrokerCapabilities;
import io.github.pnoker.common.constant.mq.OrderingGuarantee;
import io.github.pnoker.common.mq.adapter.RawBatchListener;
import io.github.pnoker.common.mq.adapter.RawDeliveryListener;
import io.github.pnoker.common.mq.adapter.WireConfirmation;
import io.github.pnoker.common.mq.adapter.WireMqDelivery;
import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import io.github.pnoker.common.mq.listener.MqPoisonException;
import io.github.pnoker.common.mq.message.WireMqMessage;
import io.github.pnoker.common.constant.mq.ConsumptionProfile;
import io.github.pnoker.common.mq.subscription.SubscriptionSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareBatchMessageListener;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.retry.MessageBatchRecoverer;
import org.aopalliance.aop.Advice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RabbitMQ implementation of the broker port. Publishes pre-serialized wire messages
 * (byte-for-byte the same JSON + headers the pre-port Jackson converter produced,
 * including the legacy {@code __TypeId__} header for rolling upgrades) and builds one
 * listener container per subscription with the same concurrency/prefetch/ack/retry
 * semantics the pre-port container factories had.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
public class RabbitMqAdapter implements BrokerAdapter {

    /**
     * Legacy Spring AMQP type header, still stamped so pre-port consumers can
     * deserialize messages published by the port during a rolling upgrade.
     */
    private static final String LEGACY_TYPE_HEADER = "__TypeId__";

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;
    private final ConnectionFactory connectionFactory;
    private final BatchConsumerProperties batchProperties;
    private final int driverQueueExpiresMillis;

    private final List<SimpleMessageListenerContainer> containers = new CopyOnWriteArrayList<>();

    public RabbitMqAdapter(RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin, ConnectionFactory connectionFactory,
                           BatchConsumerProperties batchProperties, int driverQueueExpiresMillis) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
        this.connectionFactory = connectionFactory;
        this.batchProperties = batchProperties;
        this.driverQueueExpiresMillis = driverQueueExpiresMillis;
        RabbitTopology.declareSharedTopology(rabbitAdmin);
    }

    @Override
    public String type() {
        return "rabbitmq";
    }

    @Override
    public BrokerCapabilities capabilities() {
        // delayedMessage=false: only the intrinsic TTL+DLX topics (STATE_TIMEOUT,
        // DEVICE_SCAN) delay server-side, and their senders do not set message delays.
        // Arbitrary per-message delays go through the port's local-scheduler fallback.
        return new BrokerCapabilities(false, true, true, true, true, true, true, OrderingGuarantee.NONE);
    }

    @Override
    public void publish(WireMqMessage message) {
        rabbitTemplate.send(exchangeOf(message.topic()), routingKeyOf(message), amqpMessage(message));
    }

    @Override
    public void publish(WireMqMessage message, WireConfirmation confirmation) {
        org.springframework.amqp.rabbit.connection.CorrelationData correlationData =
                new org.springframework.amqp.rabbit.connection.CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.send(exchangeOf(message.topic()), routingKeyOf(message), amqpMessage(message), correlationData);
        correlationData.getFuture().whenComplete((confirm, failure) -> {
            boolean routed = Objects.isNull(failure) && Objects.nonNull(confirm) && confirm.isAck()
                    && Objects.isNull(correlationData.getReturned());
            confirmation.onConfirm(message, routed, failure);
        });
    }

    @Override
    public void subscribe(SubscriptionSpec spec, RawDeliveryListener listener) {
        String queue = resolveQueue(spec);
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queue);
        container.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        applyProfile(container, spec.profile());
        container.setMessageListener((ChannelAwareMessageListener) (message, channel) -> {
            try {
                listener.onDelivery(deliveryOf(message, channel));
            } catch (MqPoisonException e) {
                throw new AmqpRejectAndDontRequeueException("Poison message dead-lettered: " + e.getMessage(), e);
            }
        });
        start(spec, queue, container);
    }

    @Override
    public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
        String queue = resolveQueue(spec);
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queue);
        container.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        container.setConcurrentConsumers(batchProperties.getConcurrentConsumers());
        container.setMaxConcurrentConsumers(batchProperties.getMaxConcurrentConsumers());
        container.setPrefetchCount(Math.max(batchProperties.getPrefetchCount(), batchProperties.getBatchSize()));
        container.setConsumerBatchEnabled(true);
        container.setBatchSize(batchProperties.getBatchSize());
        container.setBatchReceiveTimeout(batchProperties.getReceiveTimeoutMillis());
        container.setAdviceChain(batchRetryAdvice());
        container.setMessageListener((ChannelAwareBatchMessageListener) (messages, channel)
                -> handleBatch(messages, channel, listener));
        start(spec, queue, container);
    }

    private void handleBatch(List<Message> messages, Channel channel, RawBatchListener listener) {
        try {
            listener.onBatch(deliveriesOf(messages, channel));
        } catch (MqPoisonException e) {
            throw new AmqpRejectAndDontRequeueException("Poison batch dead-lettered: " + e.getMessage(), e);
        }
    }

    private void start(SubscriptionSpec spec, String queue, SimpleMessageListenerContainer container) {
        container.setAutoStartup(true);
        container.setMissingQueuesFatal(false);
        containers.add(container);
        container.start();
        log.info("RabbitMQ subscription started, topic={}, mode={}, delivery={}, queue={}",
                spec.topic(), spec.mode(), spec.delivery(), queue);
    }

    /**
     * Stop every container this adapter started (driver per-instance queues expire with
     * the lease; center-side queues survive restarts untouched).
     */
    public void stop() {
        containers.forEach(SimpleMessageListenerContainer::stop);
        containers.clear();
    }

    private void applyProfile(SimpleMessageListenerContainer container, ConsumptionProfile profile) {
        if (profile == ConsumptionProfile.THROUGHPUT) {
            container.setConcurrentConsumers(4);
            container.setMaxConcurrentConsumers(32);
            container.setPrefetchCount(100);
        } else {
            container.setConcurrentConsumers(2);
            container.setMaxConcurrentConsumers(8);
            container.setPrefetchCount(10);
        }
    }

    private Advice batchRetryAdvice() {
        MessageBatchRecoverer recoverer = (messages, cause) -> {
            log.error("Point-value batch exhausted retries, rejecting to dead-letter exchange, size={}",
                    messages.size(), cause);
            throw new AmqpRejectAndDontRequeueException("Point-value batch exhausted retries", true, cause);
        };
        return RetryInterceptorBuilder.stateless()
                .maxRetries(batchProperties.getMaxRetries())
                .backOffOptions(batchProperties.getRetryInitialIntervalMillis(),
                        batchProperties.getRetryMultiplier(), batchProperties.getRetryMaxIntervalMillis())
                .recoverer(recoverer)
                .build();
    }

    private WireMqDelivery deliveryOf(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        boolean redelivered = Boolean.TRUE.equals(message.getMessageProperties().getRedelivered());
        return new WireMqDelivery(message.getBody(), headersOf(message), redelivered,
                RabbitAcknowledgment.single(channel, deliveryTag));
    }

    private List<WireMqDelivery> deliveriesOf(List<Message> messages, Channel channel) {
        if (messages.isEmpty()) {
            return List.of();
        }
        long lastTag = messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag();
        RabbitAcknowledgment acknowledgment = RabbitAcknowledgment.batch(channel, lastTag);
        List<WireMqDelivery> deliveries = new ArrayList<>(messages.size());
        for (Message message : messages) {
            boolean redelivered = Boolean.TRUE.equals(message.getMessageProperties().getRedelivered());
            deliveries.add(new WireMqDelivery(message.getBody(), headersOf(message), redelivered, acknowledgment));
        }
        return deliveries;
    }

    private Map<String, String> headersOf(Message message) {
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> raw = message.getMessageProperties().getHeaders();
        if (Objects.nonNull(raw)) {
            raw.forEach((key, value) -> headers.put(key, Objects.nonNull(value) ? String.valueOf(value) : null));
        }
        String correlationId = message.getMessageProperties().getCorrelationId();
        if (Objects.nonNull(correlationId)) {
            headers.put(MqHeaders.CORRELATION_ID, correlationId);
        }
        return headers;
    }

    private Message amqpMessage(WireMqMessage wire) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setHeader(LEGACY_TYPE_HEADER, wire.headers().get(MqHeaders.DC3_TYPE));
        wire.headers().forEach(properties::setHeader);
        String correlationId = wire.headers().get(MqHeaders.CORRELATION_ID);
        if (Objects.nonNull(correlationId)) {
            properties.setCorrelationId(correlationId);
        }
        return new Message(wire.body(), properties);
    }

    private String exchangeOf(MqTopic topic) {
        return switch (topic) {
            case STATE -> RabbitNames.EXCHANGE_STATE;
            case ALARM, NOTIFY_TASK -> RabbitNames.EXCHANGE_ALARM;
            case METADATA -> RabbitNames.EXCHANGE_METADATA;
            case POINT_COMMAND -> RabbitNames.EXCHANGE_POINT_COMMAND;
            case POINT_VALUE -> RabbitNames.EXCHANGE_VALUE;
            case EVENT -> RabbitNames.EXCHANGE_EVENT;
            case COMMAND -> RabbitNames.EXCHANGE_COMMAND;
            case COMMAND_RESULT -> RabbitNames.EXCHANGE_COMMAND_RESULT;
            case POINT_COMMAND_RESULT -> RabbitNames.EXCHANGE_POINT_COMMAND_RESULT;
            case STATE_TIMEOUT, DEVICE_SCAN -> RabbitNames.EXCHANGE_STATE_TIMEOUT_DELAY;
            case POINT_VALUE_DEAD, POINT_COMMAND_DEAD, COMMAND_DEAD ->
                    throw new IllegalArgumentException("Dead-letter topics are not publishable: " + topic);
        };
    }

    private String routingKeyOf(WireMqMessage message) {
        String key = Objects.toString(message.partitionKey(), "");
        return switch (message.topic()) {
            case STATE -> RabbitNames.ROUTING_STATE_PREFIX + key;
            case ALARM -> RabbitNames.ROUTING_ALARM_PREFIX + key;
            case METADATA -> RabbitNames.ROUTING_DRIVER_METADATA_PREFIX + key;
            case POINT_COMMAND -> RabbitNames.ROUTING_POINT_COMMAND_PREFIX + key;
            case POINT_VALUE -> RabbitNames.ROUTING_POINT_VALUE_PREFIX + key;
            case EVENT -> RabbitNames.ROUTING_EVENT_PREFIX + key;
            case COMMAND -> RabbitNames.ROUTING_COMMAND_PREFIX + key;
            case COMMAND_RESULT -> RabbitNames.ROUTING_COMMAND_RESULT_PREFIX + key;
            case POINT_COMMAND_RESULT -> RabbitNames.ROUTING_POINT_COMMAND_RESULT_PREFIX + key;
            case NOTIFY_TASK -> RabbitNames.ROUTING_NOTIFY_TASK_PREFIX + key;
            case STATE_TIMEOUT -> RabbitNames.ROUTING_DRIVER_TIMEOUT_DELAY;
            case DEVICE_SCAN -> RabbitNames.ROUTING_DEVICE_SCAN_TICK;
            case POINT_VALUE_DEAD, POINT_COMMAND_DEAD, COMMAND_DEAD ->
                    throw new IllegalArgumentException("Dead-letter topics are not publishable: " + message.topic());
        };
    }


    /**
     * Blank group resolves to the platform-shared queue (pre-port name); a named group
     * gets a group-suffixed copy of the same queue for named consumer groups.
     */
    private String grouped(String baseQueue, String group) {
        if (Objects.isNull(group) || group.isBlank()) {
            return baseQueue;
        }
        return RabbitTopology.declareGroupedQueue(rabbitAdmin, baseQueue, group);
    }

    private String resolveQueue(SubscriptionSpec spec) {
        String group = spec.group();
        String keyPattern = spec.keyPattern();
        return switch (spec.topic()) {
            case STATE -> switch (keyPattern) {
                case "driver.*" -> grouped(RabbitNames.QUEUE_DRIVER_STATE, group);
                case "device.*" -> grouped(RabbitNames.QUEUE_DEVICE_STATE, group);
                default -> throw new IllegalArgumentException(
                        "STATE subscription requires keyPattern driver.* or device.*, got: " + keyPattern);
            };
            case ALARM -> switch (keyPattern) {
                case "driver.*" -> grouped(RabbitNames.QUEUE_DRIVER_ALARM, group);
                case "device.*" -> grouped(RabbitNames.QUEUE_DEVICE_ALARM, group);
                case "task.*" -> grouped(RabbitNames.QUEUE_NOTIFY_TASK, group);
                default -> throw new IllegalArgumentException(
                        "ALARM subscription requires keyPattern driver.*, device.* or task.*, got: " + keyPattern);
            };
            case METADATA -> {
                RabbitTopology.declareMetadataQueue(rabbitAdmin, group,
                        RabbitNames.ROUTING_DRIVER_METADATA_PREFIX + keyPattern);
                yield RabbitNames.QUEUE_DRIVER_METADATA_PREFIX + group;
            }
            case POINT_VALUE -> grouped(RabbitNames.QUEUE_POINT_VALUE, group);
            case EVENT -> grouped(RabbitNames.QUEUE_EVENT_REPORT, group);
            case COMMAND -> {
                RabbitTopology.declareDriverCommandQueue(rabbitAdmin, RabbitNames.QUEUE_COMMAND_PREFIX + group,
                        RabbitNames.EXCHANGE_COMMAND, RabbitNames.EXCHANGE_COMMAND_DEAD,
                        RabbitNames.ROUTING_COMMAND_PREFIX + keyPattern, driverQueueExpiresMillis);
                yield RabbitNames.QUEUE_COMMAND_PREFIX + group;
            }
            case POINT_COMMAND -> {
                int expires = Objects.nonNull(spec.instanceTtl()) && !spec.instanceTtl().isZero()
                        ? (int) Math.min(spec.instanceTtl().toMillis(), Integer.MAX_VALUE)
                        : driverQueueExpiresMillis;
                RabbitTopology.declareDriverCommandQueue(rabbitAdmin, RabbitNames.QUEUE_POINT_COMMAND_PREFIX + group,
                        RabbitNames.EXCHANGE_POINT_COMMAND, RabbitNames.EXCHANGE_POINT_COMMAND_DEAD,
                        RabbitNames.ROUTING_POINT_COMMAND_PREFIX + keyPattern, expires);
                yield RabbitNames.QUEUE_POINT_COMMAND_PREFIX + group;
            }
            case COMMAND_RESULT -> grouped(RabbitNames.QUEUE_COMMAND_RESULT, group);
            case POINT_COMMAND_RESULT -> grouped(RabbitNames.QUEUE_POINT_COMMAND_RESULT, group);
            case NOTIFY_TASK -> grouped(RabbitNames.QUEUE_NOTIFY_TASK, group);
            case STATE_TIMEOUT -> grouped(RabbitNames.QUEUE_DRIVER_TIMEOUT_CHECK, group);
            case DEVICE_SCAN -> grouped(RabbitNames.QUEUE_DEVICE_SCAN, group);
            case POINT_COMMAND_DEAD -> grouped(RabbitNames.QUEUE_POINT_COMMAND_DEAD, group);
            case COMMAND_DEAD -> grouped(RabbitNames.QUEUE_COMMAND_DEAD, group);
            case POINT_VALUE_DEAD -> grouped(RabbitNames.QUEUE_POINT_VALUE_DEAD, group);
        };
    }
}
