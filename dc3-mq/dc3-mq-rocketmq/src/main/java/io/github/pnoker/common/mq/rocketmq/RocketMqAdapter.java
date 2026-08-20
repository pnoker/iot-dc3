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

package io.github.pnoker.common.mq.rocketmq;

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.constant.mq.OrderingGuarantee;
import io.github.pnoker.common.constant.mq.SubscriptionMode;
import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.adapter.BrokerCapabilities;
import io.github.pnoker.common.mq.adapter.RawBatchListener;
import io.github.pnoker.common.mq.adapter.RawDeliveryListener;
import io.github.pnoker.common.mq.adapter.WireConfirmation;
import io.github.pnoker.common.mq.adapter.WireMqDelivery;
import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqPoisonException;
import io.github.pnoker.common.mq.message.WireMqMessage;
import io.github.pnoker.common.mq.subscription.SubscriptionSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RocketMQ implementation of the broker port (classic client, 4.x/5.x brokers).
 * Topics map to {@code dc3-<topic>} (RocketMQ topic names reject dots) and logical
 * dead topics to {@code dc3-<topic>-dlq}. LOAD_BALANCE rides CLUSTERING consume mode
 * with the consumer group as the competing unit, BROADCASTING fans out per instance.
 * QoS sync sends give publisher confirmation; ack maps to CONSUME_SUCCESS, reject(true)
 * to RECONSUME_LATER (broker-side bounded redelivery), reject(false) and exhaustion to
 * an explicit republish on the {@code -dlq} topic. Arbitrary delays go through the
 * port fallback (delay levels would quantize the requested duration); batches use the
 * consumer's native batch size.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
public class RocketMqAdapter implements BrokerAdapter {

    /**
     * Default consumer group when the spec carries none.
     */
    private static final String DEFAULT_GROUP = "dc3-mq";

    private final String namesrvAddr;
    private final BatchConsumerProperties retryProperties;

    private final DefaultMQProducer producer;
    private final List<DefaultMQPushConsumer> consumers = new CopyOnWriteArrayList<>();

    public RocketMqAdapter(String namesrvAddr, BatchConsumerProperties retryProperties) {
        this.namesrvAddr = namesrvAddr;
        this.retryProperties = retryProperties;
        this.producer = new DefaultMQProducer("dc3-mq-producer-" + UUID.randomUUID());
        this.producer.setNamesrvAddr(namesrvAddr);
        try {
            this.producer.start();
        } catch (MQClientException e) {
            throw new IllegalStateException("RocketMQ producer start failed", e);
        }
    }

    @Override
    public String type() {
        return "rocketmq";
    }

    @Override
    public BrokerCapabilities capabilities() {
        return new BrokerCapabilities(false, false, true, true, true, true, false, OrderingGuarantee.PER_KEY);
    }

    @Override
    public void publish(WireMqMessage message) {
        try {
            producer.send(rocketMessage(message));
        } catch (Exception e) {
            throw new IllegalStateException("RocketMQ publish failed, topic=" + message.topic(), e);
        }
    }

    @Override
    public void publish(WireMqMessage message, WireConfirmation confirmation) {
        try {
            producer.send(rocketMessage(message), new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    confirmation.onConfirm(message, Objects.nonNull(sendResult), null);
                }

                @Override
                public void onException(Throwable failure) {
                    confirmation.onConfirm(message, false, failure);
                }
            });
        } catch (Exception e) {
            confirmation.onConfirm(message, false, e);
        }
    }

    @Override
    public void subscribe(SubscriptionSpec spec, RawDeliveryListener listener) {
        DefaultMQPushConsumer consumer = consumerOf(spec, false);
        consumers.add(consumer);
        consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
            MessageExt first = messages.get(0);
            Outcome outcome = new Outcome();
            Acknowledgment ack = new RocketAcknowledgment(first, spec.topic(), outcome, this);
            try {
                listener.onDelivery(deliveryOf(first, ack));
            } catch (MqPoisonException e) {
                deadLetter(first, spec.topic());
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (Exception e) {
                if (first.getReconsumeTimes() >= Math.max(1, retryProperties.getMaxRetries())) {
                    log.error("RocketMQ delivery exhausted retries, dead-lettering, topic={}",
                            spec.topic(), e);
                    deadLetter(first, spec.topic());
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                log.warn("RocketMQ delivery failed, requeueing, topic={}", spec.topic(), e);
                context.setDelayLevelWhenNextConsume(1);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
            return outcome.status();
        });
        start(consumer, spec, 1);
    }

    @Override
    public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
        DefaultMQPushConsumer consumer = consumerOf(spec, true);
        consumers.add(consumer);
        consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
            List<MessageExt> batch = new ArrayList<>(messages);
            if (batch.isEmpty()) {
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
            Outcome outcome = new Outcome();
            Acknowledgment ack = new RocketAcknowledgment(batch.get(batch.size() - 1), spec.topic(),
                    outcome, this);
            try {
                List<WireMqDelivery> deliveries = new ArrayList<>(batch.size());
                for (MessageExt message : batch) {
                    deliveries.add(deliveryOf(message, ack));
                }
                listener.onBatch(deliveries);
            } catch (MqPoisonException e) {
                log.warn("RocketMQ poison batch dead-lettered, size={}", batch.size(), e);
                batch.forEach(message -> deadLetter(message, spec.topic()));
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            } catch (Exception e) {
                if (batch.get(0).getReconsumeTimes() >= Math.max(1, retryProperties.getMaxRetries())) {
                    log.error("RocketMQ batch exhausted retries, dead-lettering, size={}",
                            batch.size(), e);
                    batch.forEach(message -> deadLetter(message, spec.topic()));
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                log.warn("RocketMQ batch failed, requeueing, size={}", batch.size(), e);
                context.setDelayLevelWhenNextConsume(1);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
            return outcome.status();
        });
        start(consumer, spec, Math.max(1, retryProperties.getBatchSize()));
    }

    /**
     * Shut down every consumer this adapter started (the producer stays available so
     * publishing survives adapter restarts, mirroring the other adapters).
     */
    public void stop() {
        consumers.forEach(DefaultMQPushConsumer::shutdown);
        consumers.clear();
    }

    private void start(DefaultMQPushConsumer consumer, SubscriptionSpec spec, int batchSize) {
        try {
            consumer.setConsumeMessageBatchMaxSize(batchSize);
            consumer.setMaxReconsumeTimes(Math.max(1, retryProperties.getMaxRetries()) + 1);
            consumer.subscribe(topicName(spec.topic()), "*");
            if (spec.mode() == SubscriptionMode.LOAD_BALANCE) {
                seedFreshGroupToLatest(consumer.getConsumerGroup(), topicName(spec.topic()));
            }
            consumer.start();
            log.info("RocketMQ subscription started, topic={}, mode={}, group={}",
                    spec.topic(), spec.mode(), consumer.getConsumerGroup());
        } catch (MQClientException e) {
            throw new IllegalStateException("RocketMQ subscribe failed, topic=" + spec.topic(), e);
        }
    }

    private DefaultMQPushConsumer consumerOf(SubscriptionSpec spec, boolean batch) {
        String group = spec.group().isBlank() ? DEFAULT_GROUP : spec.group();
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.setInstanceName("dc3-mq-" + UUID.randomUUID());
        // CONSUME_FROM_LAST_OFFSET does not reliably skip the backlog for brand-new
        // consumer groups on existing topics; a timestamp-based start is deterministic:
        // a fresh group only sees messages published after this subscription moment,
        // mirroring the rabbit fresh-queue / kafka latest semantics. Groups with
        // committed offsets (including ones persisted at shutdown) are unaffected.
        consumer.setConsumeFromWhere(
                org.apache.rocketmq.common.consumer.ConsumeFromWhere.CONSUME_FROM_TIMESTAMP);
        consumer.setConsumeTimestamp(new java.text.SimpleDateFormat("yyyyMMddHHmmss")
                .format(new java.util.Date(System.currentTimeMillis() - 5_000)));
        if (spec.mode() == SubscriptionMode.BROADCAST) {
            consumer.setMessageModel(org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel.BROADCASTING);
        }
        return consumer;
    }

    /**
     * The 5.x classic client replays the topic backlog for brand-new consumer groups
     * regardless of consumeFromWhere, so fresh groups are seeded explicitly: every
     * queue without a committed offset is set to the current max offset, matching the
     * rabbit fresh-queue / kafka latest semantics. Groups with committed offsets
     * (including ones persisted at shutdown) are untouched.
     */
    private void seedFreshGroupToLatest(String group, String topic) {
        org.apache.rocketmq.client.consumer.DefaultMQPullConsumer seeder =
                new org.apache.rocketmq.client.consumer.DefaultMQPullConsumer(group);
        seeder.setNamesrvAddr(namesrvAddr);
        seeder.setInstanceName("dc3-mq-seed-" + UUID.randomUUID());
        try {
            seeder.start();
            java.util.Set<org.apache.rocketmq.common.message.MessageQueue> queues;
            try {
                queues = seeder.fetchSubscribeMessageQueues(topic);
            } catch (org.apache.rocketmq.client.exception.MQClientException noRoute) {
                // no route info yet: the topic has never been published to
                queues = java.util.Set.of();
            }
            if (queues.isEmpty()) {
                // topics auto-create on publish; a subscription to a not-yet-published
                // topic would wait for the 30s route refresh, so a warm-up publish
                // forces creation now. The warm-up lands before the fresh group's
                // seeded offset, so no consumer in this group ever sees it.
                producer.send(new org.apache.rocketmq.common.message.Message(topic,
                        "dc3-topic-warmup".getBytes(RemotingHelper.DEFAULT_CHARSET)));
                queues = seeder.fetchSubscribeMessageQueues(topic);
            }
            boolean seeded = false;
            for (org.apache.rocketmq.common.message.MessageQueue queue : queues) {
                // the broker reports 0 (not -1) for a group that never consumed, so both
                // 0 and -1 mean fresh; a group with a real trail has a positive offset
                if (seeder.fetchConsumeOffset(queue, true) > 0) {
                    continue;
                }
                seeder.getOffsetStore().updateOffset(queue, seeder.maxOffset(queue), false);
                seeded = true;
            }
            if (seeded) {
                seeder.getOffsetStore().persistAll(queues);
                log.info("RocketMQ fresh group seeded to latest, topic={}, group={}", topic, group);
            }
        } catch (Exception e) {
            log.warn("RocketMQ fresh-group seeding skipped, topic={}, group={}", topic, group, e);
        } finally {
            seeder.shutdown();
        }
    }

    private void deadLetter(MessageExt message, MqTopic topic) {
        try {
            Message dead = new Message(deadLetterTopic(topic), message.getBody());
            // only the standard envelope headers ride along; RocketMQ system
            // properties (CONSUME_START_TIME, UNIQ_KEY, ...) are rejected on re-publish
            for (String header : List.of(io.github.pnoker.common.mq.MqHeaders.DC3_TYPE,
                    io.github.pnoker.common.mq.MqHeaders.REQUEST_ID,
                    io.github.pnoker.common.mq.MqHeaders.TENANT_ID,
                    io.github.pnoker.common.mq.MqHeaders.CORRELATION_ID)) {
                String value = message.getUserProperty(header);
                if (Objects.nonNull(value)) {
                    dead.putUserProperty(header, value);
                }
            }
            producer.send(dead);
        } catch (Exception e) {
            log.error("RocketMQ dead-letter publish failed, topic={}", topic, e);
        }
    }

    private WireMqDelivery deliveryOf(MessageExt message, Acknowledgment acknowledgment) {
        return new WireMqDelivery(message.getBody(), headersOf(message),
                message.getReconsumeTimes() > 0, acknowledgment);
    }

    private static Map<String, String> headersOf(MessageExt message) {
        Map<String, String> headers = new HashMap<>();
        message.getProperties().forEach((key, value) -> headers.put(key, value));
        return headers;
    }

    private Message rocketMessage(WireMqMessage wire) {
        Message message = new Message(topicName(wire.topic()), wire.body());
        wire.headers().forEach((key, value) -> {
            if (Objects.nonNull(value)) {
                try {
                    message.putUserProperty(key, value);
                } catch (Exception e) {
                    log.warn("RocketMQ user property rejected, key={}", key, e);
                }
            }
        });
        return message;
    }

    /**
     * Mutable consume outcome the Acknowledgment records; defaults to success so a
     * listener that acks implicitly (or does nothing) does not loop.
     */
    private static final class Outcome {
        private ConsumeConcurrentlyStatus status = ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        ConsumeConcurrentlyStatus status() {
            return status;
        }

        void requeue() {
            status = ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }

    /**
     * ack is the default outcome; reject(true) marks the delivery for broker-side
     * bounded redelivery; reject(false) dead-letters and consumes.
     */
    private record RocketAcknowledgment(MessageExt message, MqTopic topic, Outcome outcome,
                                        RocketMqAdapter adapter) implements Acknowledgment {

        @Override
        public void ack() {
            // default CONSUME_SUCCESS
        }

        @Override
        public void reject(boolean requeue) {
            if (requeue) {
                outcome.requeue();
                return;
            }
            adapter.deadLetter(message, topic);
        }
    }

    private static String topicName(MqTopic topic) {
        return switch (topic) {
            case POINT_VALUE_DEAD -> "dc3-point_value-dlq";
            case POINT_COMMAND_DEAD -> "dc3-point_command-dlq";
            case COMMAND_DEAD -> "dc3-command-dlq";
            default -> "dc3-" + topic.name().toLowerCase();
        };
    }

    private static String deadLetterTopic(MqTopic topic) {
        return topicName(topic) + "-dlq";
    }
}
