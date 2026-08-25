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

package io.github.pnoker.common.mq.pulsar;

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
import io.github.pnoker.common.mq.subscription.KeyRoutes;
import io.github.pnoker.common.mq.subscription.SubscriptionSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar implementation of the broker port. Topics map to
 * {@code persistent://public/default/dc3-<topic>} and logical dead topics to
 * {@code dc3-<topic>.dlq}. LOAD_BALANCE rides a durable shared subscription named
 * after the consumer group (competing consumers share it, retained while offline);
 * BROADCAST rides an exclusive per-instance subscription. Fresh subscriptions start
 * at the latest position (Pulsar default), matching the rabbit fresh-queue / kafka
 * latest semantics. acknowledge/negativeAck are the ack/requeue primitives; rejecting
 * without requeue republishes to the {@code .dlq} topic (auto-created by Pulsar) and
 * acknowledges — the destination is ALWAYS the live topic name plus a {@code .dlq}
 * suffix, also for topics without a logical dead-letter enum variant, so a persistently
 * failing STATE/ALARM/EVENT message can never loop back onto its own live topic.
 * Batches use the consumer's native batchReceive; sends complete with the broker-issued
 * message id (publisher confirmation). Arbitrary delays go through the port fallback
 * for uniform behavior across brokers (native deliverAfter would also work).
 *
 * <p>Key routing (Pulsar has no binding-level key filter): LOAD_BALANCE specs sharing a
 * (topic, subscription) share ONE consumer whose deliveries are routed by
 * {@link KeyRoutes} against the message key — a blank pattern matches everything,
 * several matching listeners round-robin, and a key matching no listener in this JVM
 * is acknowledged and skipped (Rabbit unroutable-drop semantics; the message's home,
 * if any, is a matching listener on another JVM). Specs on the same (topic,
 * subscription) must agree on the delivery mode; BROADCAST specs keep an independent
 * consumer each. Single deliveries run on a small bounded daemon-thread executor per
 * subscription so the client's message-listener threads are never parked by the
 * bounded-retry backoff (acknowledges happen on the executor thread — manual acks
 * allow that).
 *
 * @author pnoker
 * @since 2026.8.20
 */
@Slf4j
public class PulsarMqAdapter implements BrokerAdapter {

    /**
     * Default subscription name when the spec carries no group.
     */
    private static final String DEFAULT_SUBSCRIPTION = "dc3-mq";

    /**
     * Backlog bound of the per-subscription delivery executor; a full bound makes the
     * client thread run the delivery itself (backpressure, never a drop).
     */
    private static final int DELIVERY_QUEUE_CAPACITY = 500;

    private final PulsarClient client;
    private final BatchConsumerProperties retryProperties;

    private final Map<String, Producer<byte[]>> producers = new ConcurrentHashMap<>();
    private final Map<String, KeyRoutes<RawDeliveryListener>> singleRoutes = new ConcurrentHashMap<>();
    private final Map<String, KeyRoutes<RawBatchListener>> batchRoutes = new ConcurrentHashMap<>();
    private final Map<String, Consumer<byte[]>> consumers = new ConcurrentHashMap<>();
    private final List<BatchPump> pumps = new CopyOnWriteArrayList<>();
    private final List<ExecutorService> deliveryExecutors = new CopyOnWriteArrayList<>();
    private volatile boolean stopped;

    public PulsarMqAdapter(PulsarClient client, BatchConsumerProperties retryProperties) {
        this.client = client;
        this.retryProperties = retryProperties;
    }

    @Override
    public String type() {
        return "pulsar";
    }

    @Override
    public BrokerCapabilities capabilities() {
        return new BrokerCapabilities(false, false, true, true, true, true, false, OrderingGuarantee.NONE);
    }

    @Override
    public void publish(WireMqMessage message) {
        try {
            producer(topicName(message.topic()))
                    .newMessage()
                    .value(message.body())
                    .key(message.partitionKey())
                    .properties(message.headers())
                    .send();
        } catch (PulsarClientException e) {
            throw new IllegalStateException("Pulsar publish failed, topic=" + message.topic(), e);
        }
    }

    @Override
    public void publish(WireMqMessage message, WireConfirmation confirmation) {
        producer(topicName(message.topic()))
                .newMessage()
                .value(message.body())
                .key(message.partitionKey())
                .properties(message.headers())
                .sendAsync()
                .whenComplete((messageId, failure) -> confirmation.onConfirm(message,
                        Objects.isNull(failure), failure));
    }

    @Override
    public void subscribe(SubscriptionSpec spec, RawDeliveryListener listener) {
        String routeKey = routeKey(spec, subscriptionName(spec));
        KeyRoutes<RawDeliveryListener> routes =
                singleRoutes.computeIfAbsent(routeKey, key -> new KeyRoutes<>());
        routes.add(spec.keyPattern(), listener);
        if (consumers.containsKey(routeKey)) {
            log.info("Pulsar subscription joined shared consumer, topic={}, mode={}, delivery={}, subscription={}",
                    spec.topic(), spec.mode(), spec.delivery(), subscriptionName(spec));
            return;
        }
        try {
            ExecutorService deliveryExecutor = deliveryExecutorOf(spec);
            Consumer<byte[]> consumer = client.newConsumer()
                    .topic(topicName(spec.topic()))
                    .subscriptionName(subscriptionName(spec))
                    .subscriptionType(spec.mode() == SubscriptionMode.BROADCAST
                            ? SubscriptionType.Exclusive : SubscriptionType.Shared)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Latest)
                    .negativeAckRedeliveryDelay(100, TimeUnit.MILLISECONDS)
                    .messageListener((consumerRef, message) -> {
                        try {
                            deliveryExecutor.execute(() -> deliverSingle(spec, consumerRef, message, routeKey));
                        } catch (RuntimeException saturated) {
                            // bounded queue full: run inline — backpressure instead of a drop
                            deliverSingle(spec, consumerRef, message, routeKey);
                        }
                    })
                    .subscribe();
            consumers.put(routeKey, consumer);
            log.info("Pulsar subscription started, topic={}, mode={}, subscription={}",
                    spec.topic(), spec.mode(), consumer.getSubscription());
        } catch (PulsarClientException e) {
            throw new IllegalStateException("Pulsar subscribe failed, topic=" + spec.topic(), e);
        }
    }

    @Override
    public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
        String routeKey = routeKey(spec, subscriptionName(spec));
        KeyRoutes<RawBatchListener> routes = batchRoutes.computeIfAbsent(routeKey, key -> new KeyRoutes<>());
        routes.add(spec.keyPattern(), listener);
        if (consumers.containsKey(routeKey)) {
            log.info("Pulsar batch subscription joined shared consumer, topic={}, mode={}, subscription={}",
                    spec.topic(), spec.mode(), subscriptionName(spec));
            return;
        }
        try {
            Consumer<byte[]> consumer = client.newConsumer()
                    .topic(topicName(spec.topic()))
                    .subscriptionName(subscriptionName(spec))
                    .subscriptionType(SubscriptionType.Shared)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Latest)
                    .negativeAckRedeliveryDelay(100, TimeUnit.MILLISECONDS)
                    .subscribe();
            consumers.put(routeKey, consumer);
            BatchPump pump = new BatchPump(spec, consumer, routes, routeKey);
            pumps.add(pump);
            Thread thread = new Thread(pump, "dc3-mq-pulsar-batch-" + spec.topic());
            thread.setDaemon(true);
            thread.start();
            log.info("Pulsar batch subscription started, topic={}, subscription={}",
                    spec.topic(), consumer.getSubscription());
        } catch (PulsarClientException e) {
            throw new IllegalStateException("Pulsar subscribeBatch failed, topic=" + spec.topic(), e);
        }
    }

    /**
     * Close every subscription, batch pump, delivery executor and producer this
     * adapter started. Idempotent and tolerant of an already-closed client (the
     * shared {@link PulsarClient} bean closes itself after the adapters).
     */
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        singleRoutes.clear();
        batchRoutes.clear();
        pumps.forEach(BatchPump::halt);
        deliveryExecutors.forEach(ExecutorService::shutdownNow);
        consumers.forEach((routeKey, consumer) -> closeQuietly(routeKey, consumer));
        consumers.clear();
        producers.forEach((topic, producer) -> {
            try {
                producer.close();
            } catch (PulsarClientException e) {
                log.debug("Pulsar producer close failed, topic={}", topic, e);
            }
        });
        producers.clear();
    }

    private void closeQuietly(String routeKey, Consumer<byte[]> consumer) {
        try {
            consumer.close();
        } catch (PulsarClientException e) {
            log.debug("Pulsar consumer close failed, routeKey={}", routeKey, e);
        }
    }

    private ExecutorService deliveryExecutorOf(SubscriptionSpec spec) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(DELIVERY_QUEUE_CAPACITY), runnable -> {
            Thread thread = new Thread(runnable, "dc3-mq-pulsar-deliver-" + spec.topic());
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
        deliveryExecutors.add(executor);
        return executor;
    }

    /**
     * Single delivery with the shared synchronous bounded-retry semantics, running on
     * the subscription's executor (never on the client listener thread).
     */
    private void deliverSingle(SubscriptionSpec spec, Consumer<byte[]> consumer, Message<byte[]> message,
                               String routeKey) {
        KeyRoutes<RawDeliveryListener> routes = singleRoutes.get(routeKey);
        RawDeliveryListener listener = Objects.isNull(routes) ? null : routes.next(message.getKey());
        if (Objects.isNull(listener)) {
            log.debug("Pulsar message matched no listener in this JVM, acknowledging and skipping, topic={}, key={}",
                    spec.topic(), message.getKey());
            acknowledge(consumer, List.of(message));
            return;
        }
        Acknowledgment ack = new PulsarAcknowledgment(consumer, List.of(message), spec.topic(), this);
        int maxAttempts = Math.max(1, retryProperties.getMaxRetries()) + 1;
        for (int attempt = 1; ; attempt++) {
            try {
                listener.onDelivery(deliveryOf(message, ack));
                return;
            } catch (MqPoisonException e) {
                log.warn("Pulsar poison delivery dead-lettered, topic={}", spec.topic(), e);
                deadLetter(spec.topic(), consumer, List.of(message));
                return;
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    log.error("Pulsar delivery exhausted retries, dead-lettering, topic={}", spec.topic(), e);
                    deadLetter(spec.topic(), consumer, List.of(message));
                    return;
                }
                sleepBackoff(attempt);
            }
        }
    }

    /**
     * Batch drain: route each message to the listener whose pattern matches its key,
     * then apply the shared synchronous bounded-retry semantics per routed sub-batch;
     * exhaustion dead-letters the sub-batch instead of dropping it.
     */
    private final class BatchPump implements Runnable {

        private final SubscriptionSpec spec;
        private final Consumer<byte[]> consumer;
        private final KeyRoutes<RawBatchListener> routes;
        private final String routeKey;
        private volatile boolean halted;

        private BatchPump(SubscriptionSpec spec, Consumer<byte[]> consumer, KeyRoutes<RawBatchListener> routes,
                          String routeKey) {
            this.spec = spec;
            this.consumer = consumer;
            this.routes = routes;
            this.routeKey = routeKey;
        }

        void halt() {
            halted = true;
        }

        @Override
        public void run() {
            while (!halted) {
                try {
                    Messages<byte[]> received = consumer.batchReceive();
                    if (Objects.isNull(received) || received.size() == 0) {
                        continue;
                    }
                    List<Message<byte[]>> batch = new ArrayList<>(received.size());
                    for (Message<byte[]> message : received) {
                        batch.add(message);
                    }
                    deliverBatch(batch);
                } catch (PulsarClientException e) {
                    if (!halted) {
                        log.warn("Pulsar batch receive failed, topic={}", spec.topic(), e);
                    }
                }
            }
        }

        private void deliverBatch(List<Message<byte[]>> batch) {
            Map<RawBatchListener, List<Message<byte[]>>> grouped = new LinkedHashMap<>();
            for (Message<byte[]> message : batch) {
                RawBatchListener listener = routes.next(message.getKey());
                if (Objects.isNull(listener)) {
                    log.debug("Pulsar batch message matched no listener in this JVM, acknowledging and skipping, topic={}, key={}",
                            spec.topic(), message.getKey());
                    acknowledge(consumer, List.of(message));
                    continue;
                }
                grouped.computeIfAbsent(listener, key -> new ArrayList<>()).add(message);
            }
            for (Map.Entry<RawBatchListener, List<Message<byte[]>>> entry : grouped.entrySet()) {
                deliverSubBatch(entry.getKey(), entry.getValue());
            }
        }

        private void deliverSubBatch(RawBatchListener listener, List<Message<byte[]>> subBatch) {
            Acknowledgment ack = new PulsarAcknowledgment(consumer, subBatch, spec.topic(), PulsarMqAdapter.this);
            int maxAttempts = Math.max(1, retryProperties.getMaxRetries()) + 1;
            for (int attempt = 1; ; attempt++) {
                try {
                    List<WireMqDelivery> deliveries = new ArrayList<>(subBatch.size());
                    for (Message<byte[]> message : subBatch) {
                        deliveries.add(deliveryOf(message, ack));
                    }
                    listener.onBatch(deliveries);
                    break;
                } catch (MqPoisonException e) {
                    log.warn("Pulsar poison batch dead-lettered, size={}", subBatch.size(), e);
                    deadLetter(spec.topic(), consumer, subBatch);
                    break;
                } catch (Exception e) {
                    if (attempt >= maxAttempts) {
                        log.error("Pulsar batch exhausted retries, dead-lettering, size={}",
                                subBatch.size(), e);
                        deadLetter(spec.topic(), consumer, subBatch);
                        break;
                    }
                    sleepBackoff(attempt);
                }
            }
        }
    }

    private void sleepBackoff(int attempt) {
        long initial = retryProperties.getRetryInitialIntervalMillis();
        long cap = retryProperties.getRetryMaxIntervalMillis();
        long exponent = Math.min(Math.max(attempt - 1, 0), 30);
        long delay = initial >= cap ? cap : Math.min(initial * (1L << exponent), cap);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void deadLetter(MqTopic topic, Consumer<byte[]> consumer, List<Message<byte[]>> messages) {
        try {
            // ALWAYS the live topic plus .dlq — including topics without a logical
            // dead-letter enum variant (Pulsar auto-creates the topic); routing the
            // dead letter back onto the live topic would loop the failure forever
            Producer<byte[]> producer = producer(deadLetterTopicName(topic));
            List<CompletableFuture<MessageId>> sends = new ArrayList<>(messages.size());
            for (Message<byte[]> message : messages) {
                sends.add(producer.newMessage()
                        .value(message.getData())
                        .key(Objects.requireNonNullElse(message.getKey(), ""))
                        .properties(message.getProperties())
                        .sendAsync());
            }
            CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new)).join();
            acknowledge(consumer, messages);
        } catch (Exception e) {
            log.error("Pulsar dead-letter publish failed, topic={}", topic, e);
        }
    }

    /**
     * ack acknowledges the message(s); reject(true) negative-acks each for near
     * immediate redelivery; reject(false) dead-letters the whole delivery batch.
     */
    private record PulsarAcknowledgment(Consumer<byte[]> consumer, List<Message<byte[]>> messages,
                                        MqTopic topic, PulsarMqAdapter adapter) implements Acknowledgment {

        @Override
        public void ack() {
            acknowledge(consumer, messages);
        }

        @Override
        public void reject(boolean requeue) {
            if (requeue) {
                messages.forEach(consumer::negativeAcknowledge);
                return;
            }
            adapter.deadLetter(topic, consumer, messages);
        }
    }

    private static void acknowledge(Consumer<byte[]> consumer, List<Message<byte[]>> messages) {
        try {
            consumer.acknowledge(messages.stream().map(Message::getMessageId).toList());
        } catch (PulsarClientException e) {
            log.warn("Pulsar acknowledge failed", e);
        }
    }

    private Producer<byte[]> producer(String topic) {
        return producers.computeIfAbsent(topic, key -> {
            try {
                return client.newProducer()
                        .topic(key)
                        .enableBatching(false)
                        .create();
            } catch (PulsarClientException e) {
                throw new IllegalStateException("Pulsar producer create failed, topic=" + key, e);
            }
        });
    }

    private WireMqDelivery deliveryOf(Message<byte[]> message, Acknowledgment acknowledgment) {
        return new WireMqDelivery(message.getData(), headersOf(message),
                message.getRedeliveryCount() > 0, acknowledgment);
    }

    private static Map<String, String> headersOf(Message<byte[]> message) {
        Map<String, String> headers = new HashMap<>(message.getProperties());
        return headers;
    }

    private static String subscriptionName(SubscriptionSpec spec) {
        if (spec.mode() == SubscriptionMode.BROADCAST) {
            // per-instance subscription: every instance receives its own copy
            return (spec.group().isBlank() ? DEFAULT_SUBSCRIPTION : spec.group()) + "-" + UUID.randomUUID();
        }
        return spec.group().isBlank() ? DEFAULT_SUBSCRIPTION : spec.group();
    }

    /**
     * Router key for one shared consumer: topic + subscription + delivery mode.
     */
    private static String routeKey(SubscriptionSpec spec, String subscription) {
        return spec.topic() + "|" + subscription + "|" + spec.delivery();
    }

    private static String topicName(MqTopic topic) {
        return "persistent://public/default/" + physicalName(topic);
    }

    /**
     * Dead-letter destination: the live topic's physical name plus {@code .dlq}; the
     * logical dead-letter enum variants resolve to exactly the same name, so producers
     * and dead-letter subscriptions land on one topic.
     */
    private static String deadLetterTopicName(MqTopic topic) {
        return "persistent://public/default/" + physicalName(topic) + ".dlq";
    }

    private static String physicalName(MqTopic topic) {
        return switch (topic) {
            case POINT_VALUE_DEAD -> "dc3-point_value.dlq";
            case POINT_COMMAND_DEAD -> "dc3-point_command.dlq";
            case COMMAND_DEAD -> "dc3-command.dlq";
            default -> "dc3-" + topic.name().toLowerCase();
        };
    }
}
