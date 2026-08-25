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
import io.github.pnoker.common.mq.MqHeaders;
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
import io.github.pnoker.common.mq.subscription.KeyRoutes;
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
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
 * <p>Key routing (RocketMQ has no binding-level key filter): LOAD_BALANCE specs
 * sharing a (topic, group) share ONE consumer whose deliveries are routed by
 * {@link KeyRoutes} against the message keys — a blank pattern matches everything,
 * several matching listeners round-robin, and a key matching no listener in this JVM
 * is consumed and skipped (Rabbit unroutable-drop semantics; the message's home, if
 * any, is a matching listener on another JVM). Specs on the same (topic, group) must
 * agree on the delivery mode; BROADCAST specs keep an independent consumer each. The
 * partition key rides the wire as the RocketMQ message key, and {@code redelivered}
 * mirrors {@code getReconsumeTimes() > 0}.
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

    /**
     * User property stamping the topic warm-up probe; delivery paths skip messages
     * carrying it so business listeners on any group never parse the probe body.
     */
    private static final String WARMUP_MARKER = "dc3-warmup";

    /**
     * Envelope dc3-type stamped on the warm-up probe — claimed by no business listener.
     */
    private static final String WARMUP_TYPE = "dc3.warmup";

    private final String namesrvAddr;
    private final BatchConsumerProperties retryProperties;

    private final DefaultMQProducer producer;
    private final Map<String, KeyRoutes<RawDeliveryListener>> singleRoutes = new ConcurrentHashMap<>();
    private final Map<String, KeyRoutes<RawBatchListener>> batchRoutes = new ConcurrentHashMap<>();
    private final Map<String, DefaultMQPushConsumer> consumers = new ConcurrentHashMap<>();
    private volatile boolean stopped;

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

    private static String groupOf(SubscriptionSpec spec) {
        return spec.group().isBlank() ? DEFAULT_GROUP : spec.group();
    }

    /**
     * Router key for one shared consumer: topic + group + delivery mode; a BROADCAST
     * spec always gets its own per-instance key (BROADCASTING consumers must not
     * compete for messages).
     */
    private static String routeKey(SubscriptionSpec spec) {
        String group = spec.mode() == SubscriptionMode.BROADCAST
                ? groupOf(spec) + "-broadcast-" + UUID.randomUUID()
                : groupOf(spec);
        return spec.topic() + "|" + group + "|" + spec.delivery();
    }

    /**
     * The partition key rides the wire as the RocketMQ message key.
     */
    private static String keyOf(MessageExt message) {
        return message.getKeys();
    }

    /**
     * The warm-up probe (topic auto-creation) is stamped with a marker property and a
     * {@code dc3-type} no business listener claims; every delivery path skips it.
     */
    private static boolean isWarmup(MessageExt message) {
        return "1".equals(message.getUserProperty(WARMUP_MARKER));
    }

    /**
     * Business headers only: the broker/client stamp a set of system properties
     * (UNIQ_KEY, CONSUME_START_TIME, CLUSTER, ...) into the same properties map, and
     * those must not leak into the port envelope.
     */
    private static Map<String, String> headersOf(MessageExt message) {
        Map<String, String> headers = new HashMap<>();
        message.getProperties().forEach((key, value) -> {
            if (!MessageConst.STRING_HASH_SET.contains(key)) {
                headers.put(key, value);
            }
        });
        return headers;
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

    @Override
    public String type() {
        return "rocketmq";
    }

    @Override
    public BrokerCapabilities capabilities() {
        // ordering NONE: message keys are carried (FIX routing) but the classic client's
        // default send does not pin same-key messages to one queue, so per-key ordering
        // is not guaranteed — only kafka's key→partition mapping guarantees it
        return new BrokerCapabilities(false, false, true, true, true, true, false, OrderingGuarantee.NONE);
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
        String routeKey = routeKey(spec);
        KeyRoutes<RawDeliveryListener> routes =
                singleRoutes.computeIfAbsent(routeKey, key -> new KeyRoutes<>());
        routes.add(spec.keyPattern(), listener);
        if (consumers.containsKey(routeKey)) {
            log.info("RocketMQ subscription joined shared consumer, topic={}, mode={}, delivery={}, group={}",
                    spec.topic(), spec.mode(), spec.delivery(), groupOf(spec));
            return;
        }
        DefaultMQPushConsumer consumer = consumerOf(spec);
        consumers.put(routeKey, consumer);
        consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
            Outcome outcome = new Outcome();
            for (MessageExt message : messages) {
                if (isWarmup(message)) {
                    continue;
                }
                deliverSingle(message, spec.topic(), routes, outcome);
                if (outcome.requeued()) {
                    break;
                }
            }
            return outcome.status();
        });
        start(consumer, spec, 1);
    }

    @Override
    public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
        String routeKey = routeKey(spec);
        KeyRoutes<RawBatchListener> routes = batchRoutes.computeIfAbsent(routeKey, key -> new KeyRoutes<>());
        routes.add(spec.keyPattern(), listener);
        if (consumers.containsKey(routeKey)) {
            log.info("RocketMQ batch subscription joined shared consumer, topic={}, mode={}, delivery={}, group={}",
                    spec.topic(), spec.mode(), spec.delivery(), groupOf(spec));
            return;
        }
        DefaultMQPushConsumer consumer = consumerOf(spec);
        consumers.put(routeKey, consumer);
        consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
            Outcome outcome = new Outcome();
            deliverBatch(messages, spec.topic(), routes, outcome);
            return outcome.status();
        });
        start(consumer, spec, Math.max(1, retryProperties.getBatchSize()));
    }

    private void deliverSingle(MessageExt message, MqTopic topic, KeyRoutes<RawDeliveryListener> routes,
                               Outcome outcome) {
        RawDeliveryListener listener = routes.next(keyOf(message));
        if (Objects.isNull(listener)) {
            log.debug("RocketMQ message matched no listener in this JVM, consuming and skipping, topic={}, key={}",
                    topic, keyOf(message));
            return;
        }
        Acknowledgment ack = new RocketAcknowledgment(List.of(message), topic, outcome, this);
        try {
            listener.onDelivery(deliveryOf(message, ack));
        } catch (MqPoisonException e) {
            log.warn("RocketMQ poison delivery dead-lettered, topic={}", topic, e);
            deadLetterOrFail(message, topic, outcome);
        } catch (Exception e) {
            if (message.getReconsumeTimes() >= Math.max(1, retryProperties.getMaxRetries())) {
                log.error("RocketMQ delivery exhausted retries, dead-lettering, topic={}", topic, e);
                deadLetterOrFail(message, topic, outcome);
                return;
            }
            log.warn("RocketMQ delivery failed, requeueing, topic={}", topic, e);
            outcome.requeue();
        }
    }

    private void deliverBatch(List<MessageExt> messages, MqTopic topic, KeyRoutes<RawBatchListener> routes,
                              Outcome outcome) {
        Map<RawBatchListener, List<MessageExt>> grouped = new LinkedHashMap<>();
        for (MessageExt message : messages) {
            if (isWarmup(message)) {
                continue;
            }
            RawBatchListener listener = routes.next(keyOf(message));
            if (Objects.isNull(listener)) {
                log.debug("RocketMQ batch message matched no listener in this JVM, consuming and skipping, topic={}, key={}",
                        topic, keyOf(message));
                continue;
            }
            grouped.computeIfAbsent(listener, key -> new ArrayList<>()).add(message);
        }
        for (Map.Entry<RawBatchListener, List<MessageExt>> entry : grouped.entrySet()) {
            deliverSubBatch(entry.getKey(), entry.getValue(), topic, outcome);
            if (outcome.requeued()) {
                return;
            }
        }
    }

    private void deliverSubBatch(RawBatchListener listener, List<MessageExt> batch, MqTopic topic, Outcome outcome) {
        // the acknowledgment wraps EVERY message of the (routed) batch: rejecting the
        // batch dead-letters all of its messages, not just the last one
        Acknowledgment ack = new RocketAcknowledgment(batch, topic, outcome, this);
        try {
            List<WireMqDelivery> deliveries = new ArrayList<>(batch.size());
            for (MessageExt message : batch) {
                deliveries.add(deliveryOf(message, ack));
            }
            listener.onBatch(deliveries);
        } catch (MqPoisonException e) {
            log.warn("RocketMQ poison batch dead-lettered, size={}", batch.size(), e);
            batch.forEach(message -> deadLetterOrFail(message, topic, outcome));
        } catch (Exception e) {
            if (batch.get(0).getReconsumeTimes() >= Math.max(1, retryProperties.getMaxRetries())) {
                log.error("RocketMQ batch exhausted retries, dead-lettering, size={}",
                        batch.size(), e);
                batch.forEach(message -> deadLetterOrFail(message, topic, outcome));
                return;
            }
            log.warn("RocketMQ batch failed, requeueing, size={}", batch.size(), e);
            outcome.requeue();
        }
    }

    /**
     * Dead-letter one message; a failed dead-letter publish requeues the delivery
     * instead of acknowledging the message away (a failed DLQ send must not lose it).
     */
    private void deadLetterOrFail(MessageExt message, MqTopic topic, Outcome outcome) {
        if (!deadLetter(message, topic)) {
            outcome.requeue();
        }
    }

    /**
     * Shut down every consumer and the producer this adapter owns. Idempotent.
     */
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        singleRoutes.clear();
        batchRoutes.clear();
        consumers.values().forEach(DefaultMQPushConsumer::shutdown);
        consumers.clear();
        producer.shutdown();
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

    private DefaultMQPushConsumer consumerOf(SubscriptionSpec spec) {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(groupOf(spec));
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
                // forces creation now. The probe carries the warm-up marker and a
                // dc3-type no business listener claims, and delivery paths skip it, so
                // no consumer on any group ever parses it.
                Message warmup = new Message(topic, "dc3-topic-warmup".getBytes(RemotingHelper.DEFAULT_CHARSET));
                warmup.setKeys("dc3-warmup");
                warmup.putUserProperty(WARMUP_MARKER, "1");
                warmup.putUserProperty(MqHeaders.DC3_TYPE, WARMUP_TYPE);
                producer.send(warmup);
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

    /**
     * Republish one message on the topic's dead-letter queue, carrying every business
     * header and the original message keys.
     *
     * @return true when the broker accepted the dead-letter message
     */
    private boolean deadLetter(MessageExt message, MqTopic topic) {
        try {
            Message dead = new Message(deadLetterTopic(topic), message.getBody());
            if (Objects.nonNull(message.getKeys()) && !message.getKeys().isBlank()) {
                dead.setKeys(message.getKeys());
            }
            // every business header rides along; RocketMQ system properties
            // (CONSUME_START_TIME, UNIQ_KEY, ...) are filtered by headersOf and are
            // rejected on re-publish anyway
            headersOf(message).forEach(dead::putUserProperty);
            producer.send(dead);
            return true;
        } catch (Exception e) {
            log.error("RocketMQ dead-letter publish failed, topic={}", topic, e);
            return false;
        }
    }

    private WireMqDelivery deliveryOf(MessageExt message, Acknowledgment acknowledgment) {
        return new WireMqDelivery(message.getBody(), headersOf(message),
                message.getReconsumeTimes() > 0, acknowledgment);
    }

    private Message rocketMessage(WireMqMessage wire) {
        Message message = new Message(topicName(wire.topic()), wire.body());
        if (Objects.nonNull(wire.partitionKey()) && !wire.partitionKey().isBlank()) {
            message.setKeys(wire.partitionKey());
        }
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

        boolean requeued() {
            return status == ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }

    /**
     * ack is the default outcome; reject(true) marks the delivery for broker-side
     * bounded redelivery; reject(false) dead-letters every message of the batch (the
     * acknowledgment wraps the whole routed batch, so no message is dropped) and
     * requeues instead when the dead-letter publish failed.
     */
    private record RocketAcknowledgment(List<MessageExt> messages, MqTopic topic, Outcome outcome,
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
            messages.forEach(message -> {
                if (!adapter.deadLetter(message, topic)) {
                    outcome.requeue();
                }
            });
        }
    }
}
