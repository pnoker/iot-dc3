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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar implementation of the broker port. Topics map to
 * {@code persistent://public/default/dc3-<topic>} and logical dead topics to
 * {@code dc3-<topic>-dlq}. LOAD_BALANCE rides a durable shared subscription named
 * after the consumer group (competing consumers share it, retained while offline);
 * BROADCAST rides an exclusive per-instance subscription. Fresh subscriptions start
 * at the latest position (Pulsar default), matching the rabbit fresh-queue / kafka
 * latest semantics. acknowledge/negativeAck are the ack/requeue primitives; rejecting
 * without requeue republishes to the {@code -dlq} topic and acknowledges; batches use
 * the consumer's native batchReceive; sends complete with the broker-issued message
 * id (publisher confirmation). Arbitrary delays go through the port fallback for
 * uniform behavior across brokers (native deliverAfter would also work).
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

    private final PulsarClient client;
    private final BatchConsumerProperties retryProperties;

    private final Map<MqTopic, Producer<byte[]>> producers = new ConcurrentHashMap<>();
    private final List<Consumer<byte[]>> consumers = new CopyOnWriteArrayList<>();
    private final List<BatchPump> pumps = new CopyOnWriteArrayList<>();

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
            producer(message.topic()).newMessage()
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
        producer(message.topic()).newMessage()
                .value(message.body())
                .key(message.partitionKey())
                .properties(message.headers())
                .sendAsync()
                .whenComplete((messageId, failure) -> confirmation.onConfirm(message,
                        Objects.isNull(failure), failure));
    }

    @Override
    public void subscribe(SubscriptionSpec spec, RawDeliveryListener listener) {
        try {
            Consumer<byte[]> consumer = client.newConsumer()
                    .topic(topicName(spec.topic()))
                    .subscriptionName(subscriptionName(spec))
                    .subscriptionType(spec.mode() == SubscriptionMode.BROADCAST
                            ? SubscriptionType.Exclusive : SubscriptionType.Shared)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Latest)
                    .negativeAckRedeliveryDelay(100, TimeUnit.MILLISECONDS)
                    .messageListener((consumerRef, message) ->
                            deliverSingle(spec, consumerRef, message, listener))
                    .subscribe();
            consumers.add(consumer);
            log.info("Pulsar subscription started, topic={}, mode={}, subscription={}",
                    spec.topic(), spec.mode(), consumer.getSubscription());
        } catch (PulsarClientException e) {
            throw new IllegalStateException("Pulsar subscribe failed, topic=" + spec.topic(), e);
        }
    }

    @Override
    public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
        try {
            Consumer<byte[]> consumer = client.newConsumer()
                    .topic(topicName(spec.topic()))
                    .subscriptionName(subscriptionName(spec))
                    .subscriptionType(SubscriptionType.Shared)
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Latest)
                    .negativeAckRedeliveryDelay(100, TimeUnit.MILLISECONDS)
                    .subscribe();
            consumers.add(consumer);
            BatchPump pump = new BatchPump(spec, consumer, listener);
            pumps.add(pump);
            Thread thread = new Thread(pump, "dc3-mq-pulsar-batch-" + spec.topic());
            thread.start();
            log.info("Pulsar batch subscription started, topic={}, subscription={}",
                    spec.topic(), consumer.getSubscription());
        } catch (PulsarClientException e) {
            throw new IllegalStateException("Pulsar subscribeBatch failed, topic=" + spec.topic(), e);
        }
    }

    /**
     * Close every subscription and batch pump this adapter started; the client and
     * producers stay open so publishing survives adapter restarts (mirroring the
     * other adapters and the durability contract case).
     */
    public void stop() {
        pumps.forEach(BatchPump::halt);
        consumers.forEach(consumer -> {
            try {
                consumer.close();
            } catch (PulsarClientException e) {
                log.debug("Pulsar consumer close failed", e);
            }
        });
        consumers.clear();
    }

    private void deliverSingle(SubscriptionSpec spec, Consumer<byte[]> consumer, Message<byte[]> message,
                               RawDeliveryListener listener) {
        Acknowledgment ack = new PulsarAcknowledgment(consumer, List.of(message), spec.topic(), this);
        int maxAttempts = Math.max(1, retryProperties.getMaxRetries()) + 1;
        for (int attempt = 1; ; attempt++) {
            try {
                listener.onDelivery(deliveryOf(message, ack));
                return;
            } catch (MqPoisonException e) {
                deadLetter(spec, consumer, List.of(message));
                return;
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    log.error("Pulsar delivery exhausted retries, dead-lettering, topic={}", spec.topic(), e);
                    deadLetter(spec, consumer, List.of(message));
                    return;
                }
                sleepBackoff(attempt);
            }
        }
    }

    /**
     * Batch drain: the shared synchronous bounded-retry semantics; exhaustion
     * dead-letters the whole batch instead of dropping it.
     */
    private final class BatchPump implements Runnable {

        private final SubscriptionSpec spec;
        private final Consumer<byte[]> consumer;
        private final RawBatchListener listener;
        private volatile boolean halted;

        private BatchPump(SubscriptionSpec spec, Consumer<byte[]> consumer, RawBatchListener listener) {
            this.spec = spec;
            this.consumer = consumer;
            this.listener = listener;
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
                    Acknowledgment ack = new PulsarAcknowledgment(consumer, batch, spec.topic(),
                            PulsarMqAdapter.this);
                    int maxAttempts = Math.max(1, retryProperties.getMaxRetries()) + 1;
                    for (int attempt = 1; ; attempt++) {
                        try {
                            List<WireMqDelivery> deliveries = new ArrayList<>(batch.size());
                            for (Message<byte[]> message : batch) {
                                deliveries.add(deliveryOf(message, ack));
                            }
                            listener.onBatch(deliveries);
                            break;
                        } catch (MqPoisonException e) {
                            log.warn("Pulsar poison batch dead-lettered, size={}", batch.size(), e);
                            deadLetter(spec, consumer, batch);
                            break;
                        } catch (Exception e) {
                            if (attempt >= maxAttempts) {
                                log.error("Pulsar batch exhausted retries, dead-lettering, size={}",
                                        batch.size(), e);
                                deadLetter(spec, consumer, batch);
                                break;
                            }
                            sleepBackoff(attempt);
                        }
                    }
                } catch (PulsarClientException e) {
                    if (!halted) {
                        log.warn("Pulsar batch receive failed, topic={}", spec.topic(), e);
                    }
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

    private void deadLetter(SubscriptionSpec spec, Consumer<byte[]> consumer, List<Message<byte[]>> messages) {
        try {
            Producer<byte[]> producer = producer(deadTopic(spec.topic()));
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
            log.error("Pulsar dead-letter publish failed, topic={}", spec.topic(), e);
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
            adapter.deadLetter(specOf(topic), consumer, messages);
        }

        private static SubscriptionSpec specOf(MqTopic topic) {
            return SubscriptionSpec.of(topic, byte[].class);
        }
    }

    private static void acknowledge(Consumer<byte[]> consumer, List<Message<byte[]>> messages) {
        try {
            consumer.acknowledge(messages.stream().map(Message::getMessageId).toList());
        } catch (PulsarClientException e) {
            log.warn("Pulsar acknowledge failed", e);
        }
    }

    private Producer<byte[]> producer(MqTopic topic) {
        return producers.computeIfAbsent(topic, key -> {
            try {
                return client.newProducer()
                        .topic(topicName(key))
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

    private static String topicName(MqTopic topic) {
        return "persistent://public/default/" + physicalName(topic);
    }

    private static String deadTopicName(MqTopic topic) {
        return "persistent://public/default/" + physicalName(topic) + "-dlq";
    }

    private static String physicalName(MqTopic topic) {
        return switch (topic) {
            case POINT_VALUE_DEAD -> "dc3-point_value-dlq";
            case POINT_COMMAND_DEAD -> "dc3-point_command-dlq";
            case COMMAND_DEAD -> "dc3-command-dlq";
            default -> "dc3-" + topic.name().toLowerCase();
        };
    }

    private static MqTopic deadTopic(MqTopic topic) {
        return switch (topic) {
            case POINT_VALUE -> MqTopic.POINT_VALUE_DEAD;
            case POINT_COMMAND -> MqTopic.POINT_COMMAND_DEAD;
            case COMMAND -> MqTopic.COMMAND_DEAD;
            default -> topic;
        };
    }
}
