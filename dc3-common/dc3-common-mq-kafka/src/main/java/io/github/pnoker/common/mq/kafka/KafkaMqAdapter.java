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
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kafka implementation of the broker port. Topics map to {@code dc3.<topic>} (logical
 * dead-letter topics map to {@code dc3.<topic>.dlq}, so rejects and dead-letter
 * subscriptions land on the same topic); the partition key becomes the record key
 * (per-key ordering via partitioning); LOAD_BALANCE rides consumer groups while
 * BROADCAST uses per-instance group ids. Delayed delivery is delegated to the port's
 * local-scheduler fallback (capability false); rejecting without requeue republishes
 * the record(s) to the {@code .dlq} topic and commits.
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

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final Map<String, Object> baseConsumerConfig;
    private final BatchConsumerProperties retryProperties;

    private final List<MessageListenerContainer> containers = new CopyOnWriteArrayList<>();

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
        ConcurrentMessageListenerContainer<String, byte[]> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory(spec, false),
                        containerProperties(spec));
        container.getContainerProperties().setMessageListener(
                (AcknowledgingMessageListener<String, byte[]>) (record, springAck) -> {
                    try {
                        listener.onDelivery(deliveryOf(record, List.of(record), springAck));
                    } catch (MqPoisonException e) {
                        publishDead(record);
                        springAck.acknowledge();
                    } catch (Exception e) {
                        log.warn("Kafka delivery failed, nacking for redelivery, topic={}, offset={}",
                                record.topic(), record.offset(), e);
                        springAck.nack(Duration.ofMillis(50));
                    }
                });
        start(spec, container);
    }

    @Override
    public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
        ConcurrentMessageListenerContainer<String, byte[]> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory(spec, true),
                        containerProperties(spec));
        container.getContainerProperties().setMessageListener(
                (BatchAcknowledgingMessageListener<String, byte[]>) (records, springAck) -> {
                    List<ConsumerRecord<String, byte[]>> batch = new ArrayList<>();
                    records.forEach(batch::add);
                    if (batch.isEmpty()) {
                        return;
                    }
                    // Synchronous bounded retry with backoff, mirroring the rabbit batch
                    // factory's stateless retry advice; exhaustion dead-letters the whole
                    // batch and commits instead of dropping it silently.
                    int maxAttempts = Math.max(1, retryProperties.getMaxRetries()) + 1;
                    for (int attempt = 1; ; attempt++) {
                        try {
                            listener.onBatch(batch.stream()
                                    .map(record -> deliveryOf(record, batch, springAck)).toList());
                            return;
                        } catch (MqPoisonException e) {
                            log.warn("Kafka poison batch dead-lettered, size={}", batch.size(), e);
                            batch.forEach(this::publishDead);
                            springAck.acknowledge();
                            return;
                        } catch (Exception e) {
                            if (attempt >= maxAttempts) {
                                log.error("Kafka batch exhausted retries, dead-lettering, size={}",
                                        batch.size(), e);
                                batch.forEach(this::publishDead);
                                springAck.acknowledge();
                                return;
                            }
                            sleepBackoff(attempt);
                        }
                    }
                });
        start(spec, container);
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
     * Stop every container this adapter started.
     */
    public void stop() {
        containers.forEach(MessageListenerContainer::stop);
        containers.clear();
    }

    private void start(SubscriptionSpec spec, ConcurrentMessageListenerContainer<String, byte[]> container) {
        container.setAutoStartup(true);
        container.setConcurrency(spec.profile() == ConsumptionProfile.THROUGHPUT ? 4 : 2);
        containers.add(container);
        container.start();
        log.info("Kafka subscription started, topic={}, mode={}, delivery={}, groupId={}",
                spec.topic(), spec.mode(), spec.delivery(), groupIdOf(spec));
    }

    private ConsumerFactory<String, byte[]> consumerFactory(SubscriptionSpec spec, boolean batch) {
        Map<String, Object> props = new HashMap<>(baseConsumerConfig);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, groupIdOf(spec));
        if (batch) {
            props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                    Math.max(1, retryProperties.getBatchSize()));
        }
        return new DefaultKafkaConsumerFactory<>(props);
    }

    private ContainerProperties containerProperties(SubscriptionSpec spec) {
        ContainerProperties properties = new ContainerProperties(topicName(spec.topic()));
        properties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        properties.setGroupId(groupIdOf(spec));
        return properties;
    }


    private String groupIdOf(SubscriptionSpec spec) {
        String base = spec.group().isBlank() ? TOPIC_PREFIX + spec.topic().name().toLowerCase() : spec.group();
        if (spec.mode() == SubscriptionMode.BROADCAST) {
            return base + "-" + UUID.randomUUID();
        }
        return base;
    }

    private WireMqDelivery deliveryOf(ConsumerRecord<String, byte[]> record,
                                      List<ConsumerRecord<String, byte[]>> batch, Acknowledgment springAck) {
        return new WireMqDelivery(record.value(), headersOf(record), false,
                new KafkaAcknowledgment(springAck, batch));
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

    private void publishDead(ConsumerRecord<String, byte[]> record) {
        ProducerRecord<String, byte[]> dead = new ProducerRecord<>(deadLetterTopic(record.topic()),
                record.key(), record.value());
        for (Header header : record.headers()) {
            dead.headers().add(header);
        }
        kafkaTemplate.send(dead);
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

    /**
     * Port acknowledgment over spring-kafka's handle: ack commits the offset(s),
     * reject(true) nacks for near-immediate redelivery, reject(false) republishes the
     * record(s) to the dead-letter topic and commits.
     */
    private final class KafkaAcknowledgment implements io.github.pnoker.common.mq.listener.Acknowledgment {

        private final Acknowledgment springAcknowledgment;
        private final List<ConsumerRecord<String, byte[]>> records;

        private KafkaAcknowledgment(Acknowledgment springAcknowledgment,
                                    List<ConsumerRecord<String, byte[]>> records) {
            this.springAcknowledgment = springAcknowledgment;
            this.records = records;
        }

        @Override
        public void ack() {
            springAcknowledgment.acknowledge();
        }

        @Override
        public void reject(boolean requeue) {
            if (requeue) {
                springAcknowledgment.nack(Duration.ofMillis(50));
                return;
            }
            records.forEach(KafkaMqAdapter.this::publishDead);
            springAcknowledgment.acknowledge();
        }
    }
}
