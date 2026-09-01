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
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.adapter.BrokerCapabilities;
import io.github.pnoker.common.constant.mq.DeliveryDisposition;
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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
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
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
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
 * Kafka requeues by republishing with an internal attempt header, which also drives
 * the delivery's {@code redelivered} flag and bounded dead-letter policy.
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
    private static final Duration SUBSCRIPTION_READY_TIMEOUT = Duration.ofSeconds(30);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final Map<String, Object> baseConsumerConfig;
    private final BatchConsumerProperties retryProperties;

    private final Map<String, KeyRoutes<RawDeliveryListener>> singleRoutes = new ConcurrentHashMap<>();
    private final Map<String, KeyRoutes<RawBatchListener>> batchRoutes = new ConcurrentHashMap<>();
    private final Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();
    private volatile boolean stopped;

    private static final class SubscriptionReady {

        private final int expectedConsumers;
        private final CountDownLatch latch = new CountDownLatch(1);
        private final Set<Consumer<?, ?>> assignedConsumers = java.util.Collections.newSetFromMap(
                new IdentityHashMap<>());

        private SubscriptionReady(int expectedConsumers) {
            this.expectedConsumers = expectedConsumers;
        }

        private synchronized void assigned(Consumer<?, ?> consumer) {
            assignedConsumers.add(consumer);
            if (assignedConsumers.size() >= expectedConsumers) {
                latch.countDown();
            }
        }

        private boolean await(Duration timeout) throws InterruptedException {
            return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

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
        int concurrency = concurrency(spec);
        SubscriptionReady ready = new SubscriptionReady(concurrency);
        ConcurrentMessageListenerContainer<String, byte[]> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory(groupId, false),
                        containerProperties(spec, groupId, ready));
        container.getContainerProperties().setMessageListener(
                (AcknowledgingMessageListener<String, byte[]>) (record, springAck)
                        -> deliverSingle(record, springAck, routeKey));
        start(spec, groupId, routeKey, container, concurrency, ready);
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
        int concurrency = concurrency(spec);
        SubscriptionReady ready = new SubscriptionReady(concurrency);
        ConcurrentMessageListenerContainer<String, byte[]> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory(groupId, true),
                        containerProperties(spec, groupId, ready));
        container.getContainerProperties().setMessageListener(
                (BatchAcknowledgingMessageListener<String, byte[]>) (records, springAck)
                        -> deliverBatch(records, springAck, routeKey));
        start(spec, groupId, routeKey, container, concurrency, ready);
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
        normalize(Mono.defer(() -> listener.onDelivery(deliveryOf(record))), record.topic(), record.offset())
                .flatMap(disposition -> settle(List.of(record), disposition))
                .doOnSuccess(ignored -> springAck.acknowledge())
                .doOnError(error -> log.error(
                        "Kafka delivery settlement failed; offset remains uncommitted, topic={}, offset={}",
                        record.topic(), record.offset(), error))
                .subscribe();
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
        Flux.fromIterable(grouped.entrySet())
                .concatMap(entry -> normalize(Mono.defer(() -> entry.getKey().onBatch(entry.getValue().stream()
                                .map(this::deliveryOf).toList())),
                        entry.getValue().get(0).topic(), entry.getValue().get(0).offset())
                        .flatMap(disposition -> settle(entry.getValue(), disposition)))
                .then()
                .doOnSuccess(ignored -> springAck.acknowledge())
                .doOnError(error -> log.error(
                        "Kafka batch settlement failed; offsets remain uncommitted for redelivery", error))
                .subscribe();
    }

    private Mono<DeliveryDisposition> normalize(Mono<DeliveryDisposition> completion, String topic, long offset) {
        return completion
                .onErrorResume(MqPoisonException.class, error -> {
                    log.warn("Kafka poison delivery dead-lettered, topic={}, offset={}", topic, offset, error);
                    return Mono.just(DeliveryDisposition.DEAD_LETTER);
                })
                .onErrorResume(error -> {
                    log.warn("Kafka delivery failed, scheduling broker redelivery, topic={}, offset={}",
                            topic, offset, error);
                    return Mono.just(DeliveryDisposition.REQUEUE);
                });
    }

    private Mono<Void> settle(List<ConsumerRecord<String, byte[]>> records, DeliveryDisposition disposition) {
        return switch (disposition) {
            case ACK -> Mono.empty();
            case DEAD_LETTER -> Flux.fromIterable(records).concatMap(this::publishDead).then();
            case REQUEUE -> Flux.fromIterable(records).concatMap(record ->
                    redeliveryCount(record) >= Math.max(1, retryProperties.getMaxRetries())
                            ? publishDead(record) : republish(record)).then();
        };
    }

    private Mono<Void> republish(ConsumerRecord<String, byte[]> record) {
        return publishCopy(record, record.topic(), redeliveryCount(record) + 1);
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
                       ConcurrentMessageListenerContainer<String, byte[]> container, int concurrency,
                       SubscriptionReady ready) {
        container.setAutoStartup(true);
        container.setConcurrency(concurrency);
        containers.put(routeKey, container);
        try {
            container.start();
            if (!ready.await(SUBSCRIPTION_READY_TIMEOUT)) {
                throw new IllegalStateException("Kafka subscription did not complete initial assignment within "
                        + SUBSCRIPTION_READY_TIMEOUT.toSeconds() + " seconds: topic=" + spec.topic()
                        + ", groupId=" + groupId);
            }
            log.info("Kafka subscription ready, topic={}, mode={}, delivery={}, groupId={}",
                    spec.topic(), spec.mode(), spec.delivery(), groupId);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            container.stop();
            containers.remove(routeKey, container);
            singleRoutes.remove(routeKey);
            batchRoutes.remove(routeKey);
            throw new IllegalStateException("Interrupted while awaiting Kafka partition assignment: topic="
                    + spec.topic() + ", groupId=" + groupId, error);
        } catch (RuntimeException error) {
            container.stop();
            containers.remove(routeKey, container);
            singleRoutes.remove(routeKey);
            batchRoutes.remove(routeKey);
            throw error;
        }
    }

    private int concurrency(SubscriptionSpec spec) {
        return spec.profile() == ConsumptionProfile.THROUGHPUT ? 4 : 2;
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

    private ContainerProperties containerProperties(SubscriptionSpec spec, String groupId, SubscriptionReady ready) {
        ContainerProperties properties = new ContainerProperties(topicName(spec.topic()));
        properties.setAckMode(ContainerProperties.AckMode.MANUAL);
        properties.setAsyncAcks(true);
        properties.setGroupId(groupId);
        properties.setConsumerRebalanceListener(new ConsumerAwareRebalanceListener() {
            @Override
            public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
                ready.assigned(consumer);
            }
        });
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

    private WireMqDelivery deliveryOf(ConsumerRecord<String, byte[]> record) {
        return new WireMqDelivery(record.value(), headersOf(record), redeliveryCount(record) > 0);
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

    private Mono<Void> publishDead(ConsumerRecord<String, byte[]> record) {
        return publishCopy(record, deadLetterTopic(record.topic()), redeliveryCount(record));
    }

    private Mono<Void> publishCopy(ConsumerRecord<String, byte[]> source, String targetTopic, int attempt) {
        ProducerRecord<String, byte[]> target = new ProducerRecord<>(targetTopic, source.key(), source.value());
        for (Header header : source.headers()) {
            if (!MqHeaders.REDELIVERY_COUNT.equals(header.key())) {
                target.headers().add(header);
            }
        }
        target.headers().add(new RecordHeader(MqHeaders.REDELIVERY_COUNT,
                String.valueOf(attempt).getBytes(StandardCharsets.UTF_8)));
        return Mono.fromFuture(kafkaTemplate.send(target)).then();
    }

    private int redeliveryCount(ConsumerRecord<String, byte[]> record) {
        Header header = record.headers().lastHeader(MqHeaders.REDELIVERY_COUNT);
        if (Objects.isNull(header) || Objects.isNull(header.value())) {
            return 0;
        }
        try {
            return Integer.parseInt(new String(header.value(), StandardCharsets.UTF_8));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
