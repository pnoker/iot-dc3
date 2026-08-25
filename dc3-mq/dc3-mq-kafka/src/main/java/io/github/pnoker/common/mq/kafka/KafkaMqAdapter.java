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

package io.github.pnoker.common.mq.kafka;

import io.github.pnoker.common.constant.mq.ConsumptionProfile;
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
import io.github.pnoker.common.mq.listener.MqPoisonException;
import io.github.pnoker.common.mq.message.WireMqMessage;
import io.github.pnoker.common.mq.subscription.KeyRoutes;
import io.github.pnoker.common.mq.subscription.SubscriptionSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Kafka implementation of the broker port. Topics map to {@code dc3.<topic>} (logical
 * dead-letter topics map to {@code dc3.<topic>.dlq}, so rejects and dead-letter
 * subscriptions land on the same topic); the partition key becomes the record key
 * (per-key ordering via partitioning); LOAD_BALANCE rides consumer groups while
 * BROADCAST uses per-instance group ids. Delayed delivery is delegated to the port's
 * local-scheduler fallback (capability false); rejecting without requeue republishes
 * the record(s) to the {@code .dlq} topic and commits.
 *
 * <p>Key routing (kafka has no binding-level key filter): LOAD_BALANCE specs sharing a
 * (topic, group) share ONE container whose deliveries are routed by
 * {@link KeyRoutes} against the record key — a blank pattern matches everything,
 * several matching listeners round-robin, and a key matching no listener in this JVM
 * is acknowledged and skipped (Rabbit unroutable-drop semantics; the message's home,
 * if any, is a matching listener on another JVM). Specs on the same (topic, group)
 * must agree on the delivery mode; BROADCAST specs keep an independent container each.
 * Kafka exposes no delivery count, so {@code redelivered} is always false here.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
public class KafkaMqAdapter implements BrokerAdapter {

    /**
     * Physical topic prefix; the design doc's namespace knob lands here in phase 3.
     */
    private static final String TOPIC_PREFIX = "dc3.";

    /**
     * DLQ send timeout: dead-lettering is synchronous so callers can decide between
     * committing (send ok) and leaving the offsets uncommitted for broker redelivery
     * (send failed — the message must not be lost silently).
     */
    private static final Duration DEAD_LETTER_SEND_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final Map<String, Object> baseConsumerConfig;
    private final BatchConsumerProperties retryProperties;

    private final Map<String, KeyRoutes<RawDeliveryListener>> singleRoutes = new ConcurrentHashMap<>();
    private final Map<String, KeyRoutes<RawBatchListener>> batchRoutes = new ConcurrentHashMap<>();
    private final Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();
    private volatile boolean stopped;

    public KafkaMqAdapter(KafkaTemplate<String, byte[]> kafkaTemplate, Map<String, Object> baseConsumerConfig,
                          BatchConsumerProperties retryProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.baseConsumerConfig = baseConsumerConfig;
        this.retryProperties = retryProperties;
    }

    /**
     * Build a byte-array template against the given bootstrap servers ({@code acks=all}
     * so completed send futures mean the broker accepted the record).
     *
     * @param bootstrapServers kafka bootstrap servers
     * @return configured template
     */
    public static KafkaTemplate<String, byte[]> template(String bootstrapServers) {
        Map<String, Object> producer = new HashMap<>();
        producer.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producer.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        producer.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                ByteArraySerializer.class);
        producer.put(org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG, "all");
        producer.put(org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        DefaultKafkaProducerFactory<String, byte[]> factory = new DefaultKafkaProducerFactory<>(producer);
        KafkaTemplate<String, byte[]> template = new KafkaTemplate<>(factory);
        template.setDefaultTopic(TOPIC_PREFIX + "default");
        return template;
    }

    /**
     * Base consumer configuration (bootstrap servers only) the adapter copies per
     * subscription.
     *
     * @param bootstrapServers kafka bootstrap servers
     * @return consumer config skeleton
     */
    public static Map<String, Object> consumerConfig(String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class);
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // latest mirrors the rabbit fresh-queue semantics: a brand new consumer group only sees
        // messages published after it joins, instead of replaying the topic backlog.
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return config;
    }

    /**
     * Router key for one shared container: topic + group + delivery mode — specs with a
     * different delivery mode keep their own container even on the same group.
     */
    private static String routeKey(SubscriptionSpec spec, String groupId) {
        return spec.topic() + "|" + groupId + "|" + spec.delivery();
    }

    private static String deadLetterTopic(String topic) {
        return topic + ".dlq";
    }

    /**
     * Physical topic for a logical destination; logical dead topics map to the
     * {@code .dlq} form so rejects and dead-letter subscriptions land on the same topic.
     */
    public static String topicName(MqTopic topic) {
        return switch (topic) {
            case POINT_VALUE_DEAD -> TOPIC_PREFIX + "point_value.dlq";
            case POINT_COMMAND_DEAD -> TOPIC_PREFIX + "point_command.dlq";
            case COMMAND_DEAD -> TOPIC_PREFIX + "command.dlq";
            default -> TOPIC_PREFIX + topic.name().toLowerCase();
        };
    }

    @Override
    public String type() {
        return "kafka";
    }

    @Override
    public BrokerCapabilities capabilities() {
        return new BrokerCapabilities(false, false, true, false, true, true, false, OrderingGuarantee.PER_KEY);
    }

    @Override
    public void publish(WireMqMessage message) {
        kafkaTemplate.send(producerRecord(message));
    }

    @Override
    public void publish(WireMqMessage message, WireConfirmation confirmation) {
        kafkaTemplate.send(producerRecord(message))
                .whenComplete((result, failure) -> confirmation.onConfirm(message, Objects.isNull(failure), failure));
    }

    @Override
    public void subscribe(SubscriptionSpec spec, RawDeliveryListener listener) {
        // the group id is minted once per subscription and reused for the consumer
        // factory, the container and the log — a fresh UUID per call would split one
        // BROADCAST subscription across three different (never-committed) group ids.
        String groupId = groupIdOf(spec);
        String routeKey = routeKey(spec, groupId);
        KeyRoutes<RawDeliveryListener> routes =
                singleRoutes.computeIfAbsent(routeKey, key -> new KeyRoutes<>());
        routes.add(spec.keyPattern(), listener);
        if (containers.containsKey(routeKey)) {
            log.info("Kafka subscription joined shared container, topic={}, mode={}, delivery={}, groupId={}",
                    spec.topic(), spec.mode(), spec.delivery(), groupId);
            return;
        }
        ConcurrentMessageListenerContainer<String, byte[]> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory(groupId, false),
                        containerProperties(spec, groupId));
        container.getContainerProperties().setMessageListener(
                (AcknowledgingMessageListener<String, byte[]>) (record, springAck)
                        -> deliverSingle(record, springAck, routeKey));
        start(spec, groupId, routeKey, container);
    }

    @Override
    public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
        String groupId = groupIdOf(spec);
        String routeKey = routeKey(spec, groupId);
        KeyRoutes<RawBatchListener> routes = batchRoutes.computeIfAbsent(routeKey, key -> new KeyRoutes<>());
        routes.add(spec.keyPattern(), listener);
        if (containers.containsKey(routeKey)) {
            log.info("Kafka batch subscription joined shared container, topic={}, mode={}, delivery={}, groupId={}",
                    spec.topic(), spec.mode(), spec.delivery(), groupId);
            return;
        }
        ConcurrentMessageListenerContainer<String, byte[]> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory(groupId, true),
                        containerProperties(spec, groupId));
        container.getContainerProperties().setMessageListener(
                (BatchAcknowledgingMessageListener<String, byte[]>) (records, springAck)
                        -> deliverBatch(records, springAck, routeKey));
        start(spec, groupId, routeKey, container);
    }

    /**
     * Route one record to the listener whose key pattern matches the record key; a key
     * with no matching listener in this JVM is acknowledged and skipped.
     */
    private void deliverSingle(ConsumerRecord<String, byte[]> record, Acknowledgment springAck, String routeKey) {
        KeyRoutes<RawDeliveryListener> routes = singleRoutes.get(routeKey);
        RawDeliveryListener listener = Objects.isNull(routes) ? null : routes.next(record.key());
        if (Objects.isNull(listener)) {
            log.debug("Kafka record matched no listener in this JVM, acknowledging and skipping, topic={}, key={}, offset={}",
                    record.topic(), record.key(), record.offset());
            springAck.acknowledge();
            return;
        }
        try {
            listener.onDelivery(deliveryOf(record, springAck, false));
        } catch (MqPoisonException e) {
            log.warn("Kafka poison delivery dead-lettered, topic={}, offset={}", record.topic(), record.offset(), e);
            if (publishDead(record)) {
                springAck.acknowledge();
            } else {
                // dead-letter send failed: leave the offset uncommitted so the broker
                // redelivers instead of silently losing the message
                log.error("Kafka dead-letter publish failed, offset left uncommitted for redelivery, topic={}, offset={}",
                        record.topic(), record.offset());
            }
        } catch (Exception e) {
            log.warn("Kafka delivery failed, nacking for redelivery, topic={}, offset={}",
                    record.topic(), record.offset(), e);
            springAck.nack(Duration.ofMillis(50));
        }
    }

    /**
     * Batch twin of {@link #deliverSingle}: group the received records by the routed
     * listener and deliver one sub-batch per listener with the shared synchronous
     * bounded-retry semantics; exhaustion dead-letters the sub-batch and commits only
     * when every dead-letter send succeeded.
     */
    private void deliverBatch(Iterable<ConsumerRecord<String, byte[]>> records, Acknowledgment springAck,
                              String routeKey) {
        KeyRoutes<RawBatchListener> routes = batchRoutes.get(routeKey);
        if (Objects.isNull(routes) || routes.isEmpty()) {
            springAck.acknowledge();
            return;
        }
        Map<RawBatchListener, List<ConsumerRecord<String, byte[]>>> grouped = new LinkedHashMap<>();
        int dropped = 0;
        for (ConsumerRecord<String, byte[]> record : records) {
            RawBatchListener listener = routes.next(record.key());
            if (Objects.isNull(listener)) {
                dropped++;
                log.debug("Kafka batch record matched no listener in this JVM, skipping, topic={}, key={}, offset={}",
                        record.topic(), record.key(), record.offset());
                continue;
            }
            grouped.computeIfAbsent(listener, key -> new ArrayList<>()).add(record);
        }
        if (dropped > 0) {
            log.debug("Kafka batch routing dropped {} unmatched record(s) in this JVM", dropped);
        }
        boolean consumed = true;
        for (Map.Entry<RawBatchListener, List<ConsumerRecord<String, byte[]>>> entry : grouped.entrySet()) {
            if (!deliverSubBatch(entry.getKey(), entry.getValue(), springAck)) {
                consumed = false;
            }
        }
        if (consumed) {
            springAck.acknowledge();
        } else {
            // at least one dead-letter send failed: leave the offsets uncommitted so
            // the broker redelivers the batch instead of losing it silently
            log.error("Kafka batch dead-letter publish failed; offsets left uncommitted for broker redelivery");
        }
    }

    /**
     * @return true when the sub-batch was consumed (ok or fully dead-lettered)
     */
    private boolean deliverSubBatch(RawBatchListener listener, List<ConsumerRecord<String, byte[]>> batch,
                                    Acknowledgment springAck) {
        // Synchronous bounded retry with backoff, mirroring the rabbit batch
        // factory's stateless retry advice; exhaustion dead-letters the whole
        // batch instead of dropping it silently.
        int maxAttempts = Math.max(1, retryProperties.getMaxRetries()) + 1;
        for (int attempt = 1; ; attempt++) {
            try {
                listener.onBatch(batch.stream()
                        .map(record -> deliveryOf(record, springAck, true)).toList());
                return true;
            } catch (MqPoisonException e) {
                log.warn("Kafka poison batch dead-lettered, size={}", batch.size(), e);
                return deadLetterAll(batch);
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    log.error("Kafka batch exhausted retries, dead-lettering, size={}",
                            batch.size(), e);
                    return deadLetterAll(batch);
                }
                sleepBackoff(attempt);
            }
        }
    }

    /**
     * @return true when every dead-letter send was confirmed by the broker
     */
    private boolean deadLetterAll(List<ConsumerRecord<String, byte[]>> batch) {
        boolean all = true;
        for (ConsumerRecord<String, byte[]> record : batch) {
            all &= publishDead(record);
        }
        return all;
    }

    /**
     * Exponential backoff between synchronous batch retry attempts, bounded by the
     * configured ceiling; interrupted sleep aborts the wait without failing the batch.
     */
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

    /**
     * Stop every container this adapter started. Idempotent; the KafkaTemplate is a
     * shared bean and stays open.
     */
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        singleRoutes.clear();
        batchRoutes.clear();
        containers.values().forEach(MessageListenerContainer::stop);
        containers.clear();
    }

    private void start(SubscriptionSpec spec, String groupId, String routeKey,
                       ConcurrentMessageListenerContainer<String, byte[]> container) {
        container.setAutoStartup(true);
        container.setConcurrency(spec.profile() == ConsumptionProfile.THROUGHPUT ? 4 : 2);
        containers.put(routeKey, container);
        container.start();
        log.info("Kafka subscription started, topic={}, mode={}, delivery={}, groupId={}",
                spec.topic(), spec.mode(), spec.delivery(), groupId);
    }

    private ConsumerFactory<String, byte[]> consumerFactory(String groupId, boolean batch) {
        Map<String, Object> props = new HashMap<>(baseConsumerConfig);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, groupId);
        if (batch) {
            props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                    Math.max(1, retryProperties.getBatchSize()));
        }
        return new DefaultKafkaConsumerFactory<>(props);
    }

    private ContainerProperties containerProperties(SubscriptionSpec spec, String groupId) {
        ContainerProperties properties = new ContainerProperties(topicName(spec.topic()));
        properties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        properties.setGroupId(groupId);
        return properties;
    }

    /**
     * Group id per subscription: the spec group (drivers' client id), or the
     * topic-derived default; BROADCAST gets a per-instance suffix so every broadcast
     * subscription is its own group.
     */
    private String groupIdOf(SubscriptionSpec spec) {
        String base = spec.group().isBlank() ? TOPIC_PREFIX + spec.topic().name().toLowerCase() : spec.group();
        if (spec.mode() == SubscriptionMode.BROADCAST) {
            return base + "-" + UUID.randomUUID();
        }
        return base;
    }

    private WireMqDelivery deliveryOf(ConsumerRecord<String, byte[]> record, Acknowledgment springAck, boolean batch) {
        return new WireMqDelivery(record.value(), headersOf(record), false,
                new KafkaAcknowledgment(springAck, List.of(record), batch));
    }

    private Map<String, String> headersOf(ConsumerRecord<String, byte[]> record) {
        Map<String, String> headers = new HashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), Objects.isNull(header.value()) ? null
                    : new String(header.value(), StandardCharsets.UTF_8));
        }
        return headers;
    }

    private ProducerRecord<String, byte[]> producerRecord(WireMqMessage wire) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topicName(wire.topic()),
                wire.partitionKey(), wire.body());
        wire.headers().forEach((key, value) -> record.headers().add(key,
                Objects.isNull(value) ? null : value.getBytes(StandardCharsets.UTF_8)));
        return record;
    }

    /**
     * Synchronous dead-letter publish: the caller only commits the offset when this
     * returns true; on failure the offsets stay uncommitted and the broker redelivers.
     *
     * @return true when the broker accepted the dead-letter record
     */
    private boolean publishDead(ConsumerRecord<String, byte[]> record) {
        try {
            ProducerRecord<String, byte[]> dead = new ProducerRecord<>(deadLetterTopic(record.topic()),
                    record.key(), record.value());
            for (Header header : record.headers()) {
                dead.headers().add(header);
            }
            kafkaTemplate.send(dead).get(DEAD_LETTER_SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            log.error("Kafka dead-letter publish failed, topic={}, offset={}", record.topic(), record.offset(), e);
            return false;
        }
    }

    /**
     * Port acknowledgment over spring-kafka's handle: ack commits the offset(s),
     * reject(true) nacks for near-immediate redelivery — the batch acknowledgment only
     * implements {@code nack(index, duration)} (the index-less overload throws
     * UnsupportedOperationException), so a batch reject nacks from index 0 to redeliver
     * the whole batch — and reject(false) republishes the record(s) to the dead-letter
     * topic and commits.
     */
    private final class KafkaAcknowledgment implements io.github.pnoker.common.mq.listener.Acknowledgment {

        private final Acknowledgment springAcknowledgment;
        private final List<ConsumerRecord<String, byte[]>> records;
        private final boolean batch;

        private KafkaAcknowledgment(Acknowledgment springAcknowledgment, List<ConsumerRecord<String, byte[]>> records,
                                    boolean batch) {
            this.springAcknowledgment = springAcknowledgment;
            this.records = records;
            this.batch = batch;
        }

        @Override
        public void ack() {
            springAcknowledgment.acknowledge();
        }

        @Override
        public void reject(boolean requeue) {
            if (requeue) {
                if (batch) {
                    // redeliver the whole batch: ConsumerBatchAcknowledgment only
                    // supports the indexed nack
                    springAcknowledgment.nack(0, Duration.ofMillis(50));
                } else {
                    springAcknowledgment.nack(Duration.ofMillis(50));
                }
                return;
            }
            if (deadLetterAll(records)) {
                springAcknowledgment.acknowledge();
            } else {
                log.error("Kafka dead-letter publish failed on reject, offsets left uncommitted for redelivery, records={}",
                        records.size());
            }
        }
    }
}
