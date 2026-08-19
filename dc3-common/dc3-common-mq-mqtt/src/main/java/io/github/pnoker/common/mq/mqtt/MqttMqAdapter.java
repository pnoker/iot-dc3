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

package io.github.pnoker.common.mq.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 5 implementation of the broker port (EMQX / HiveMQ / NanoMQ / VerneMQ ...).
 * Topics map to {@code dc3/<topic>} (slash style); logical dead topics map to
 * {@code dc3/<topic>/dlq}. LOAD_BALANCE rides MQTT 5 shared subscriptions
 * ({@code $share/<group>/...}), BROADCAST rides a plain subscription — every
 * subscription gets its own client session, so broadcast instances are independent.
 * QoS 1 gives per-message ack and PUBACK publisher confirmation. MQTT has no server
 * nack: reject(true) is approximated by client-side bounded redelivery; reject(false)
 * republishes to the {@code /dlq} topic and acknowledges. Delayed delivery and batch
 * are the port fallback / synthesized (capabilities false).
 *
 * <p>Known broker variance (design §13.8): MQTT 5 is silent on retention for a shared
 * subscription while no member is online — messages may be dropped. Deployers relying
 * on group durability must pick a broker that retains; the contract suite's
 * no-consumer case is disabled for this adapter with that reference.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
public class MqttMqAdapter implements BrokerAdapter {

    /**
     * Default share group when the spec carries no group.
     */
    private static final String DEFAULT_SHARE = "dc3-mq";

    private final String host;
    private final int port;
    private final BatchConsumerProperties retryProperties;

    private final Mqtt5AsyncClient publishClient;
    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    public MqttMqAdapter(String host, int port, BatchConsumerProperties retryProperties) {
        this.host = host;
        this.port = port;
        this.retryProperties = retryProperties;
        this.publishClient = client("dc3-mq-publisher-" + UUID.randomUUID());
    }

    /**
     * Client factory shared by the publisher and every subscription session.
     */
    private Mqtt5AsyncClient client(String identifier) {
        Mqtt5AsyncClient client = Mqtt5Client.builder()
                .identifier(identifier)
                .serverHost(host)
                .serverPort(port)
                .buildAsync();
        client.connect().join();
        return client;
    }

    @Override
    public String type() {
        return "mqtt";
    }

    @Override
    public BrokerCapabilities capabilities() {
        return new BrokerCapabilities(false, false, true, true, true, false, false, OrderingGuarantee.NONE);
    }

    @Override
    public void publish(WireMqMessage message) {
        // sync contract: return after the broker accepted (PUBACK for QoS 1)
        publishClient.publish(publishOf(message)).join();
    }

    @Override
    public void publish(WireMqMessage message, WireConfirmation confirmation) {
        publishClient.publish(publishOf(message))
                .whenComplete((publish, failure) -> confirmation.onConfirm(message,
                        Objects.isNull(failure), failure));
    }

    @Override
    public void subscribe(SubscriptionSpec spec, RawDeliveryListener listener) {
        Mqtt5AsyncClient client = client("dc3-mq-sub-" + UUID.randomUUID());
        Dispatcher dispatcher = new Dispatcher(spec, listener, null);
        subscriptions.add(new Subscription(client, dispatcher));
        client.subscribe(subscribeOf(spec), dispatcher::accept, true);
        dispatcher.start();
        log.info("MQTT subscription started, topic={}, mode={}, filter={}",
                spec.topic(), spec.mode(), filterOf(spec));
    }

    @Override
    public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
        Mqtt5AsyncClient client = client("dc3-mq-sub-" + UUID.randomUUID());
        Dispatcher dispatcher = new Dispatcher(spec, null, listener);
        subscriptions.add(new Subscription(client, dispatcher));
        client.subscribe(subscribeOf(spec), dispatcher::accept, true);
        dispatcher.start();
        log.info("MQTT batch subscription started, topic={}, mode={}, filter={}",
                spec.topic(), spec.mode(), filterOf(spec));
    }

    /**
     * Disconnect every subscription session and the publisher.
     */
    public void stop() {
        subscriptions.forEach(subscription -> {
            subscription.dispatcher().halt();
            subscription.client().disconnect();
        });
        subscriptions.clear();
        publishClient.disconnect();
    }

    /**
     * Per-subscription dispatch: single deliveries invoke the listener directly with
     * bounded client-side redelivery for reject(true); batch subscriptions drain the
     * incoming queue into windowed batches with the shared synchronous bounded-retry
     * semantics. Poison and exhausted messages are republished to the dead-letter
     * topic and acknowledged.
     */
    private final class Dispatcher implements Runnable {

        private final SubscriptionSpec spec;
        private final RawDeliveryListener single;
        private final RawBatchListener batch;
        private final BlockingQueue<Mqtt5Publish> incoming = new ArrayBlockingQueue<>(10_000);
        private volatile boolean halted;
        private Thread thread;

        private Dispatcher(SubscriptionSpec spec, RawDeliveryListener single, RawBatchListener batch) {
            this.spec = spec;
            this.single = single;
            this.batch = batch;
        }

        void start() {
            thread = new Thread(this, "dc3-mq-mqtt-" + spec.topic());
            thread.start();
        }

        void halt() {
            halted = true;
        }

        void accept(Mqtt5Publish publish) {
            incoming.add(publish);
        }

        @Override
        public void run() {
            while (!halted) {
                try {
                    if (Objects.nonNull(batch)) {
                        pumpBatch();
                    } else {
                        Mqtt5Publish publish = incoming.poll(200, TimeUnit.MILLISECONDS);
                        if (Objects.nonNull(publish)) {
                            deliverSingle(publish, 1);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        private void pumpBatch() throws InterruptedException {
            Mqtt5Publish first = incoming.poll(200, TimeUnit.MILLISECONDS);
            if (Objects.isNull(first)) {
                return;
            }
            List<Mqtt5Publish> publishes = new ArrayList<>(List.of(first));
            long deadline = System.currentTimeMillis() + retryProperties.getReceiveTimeoutMillis();
            while (publishes.size() < retryProperties.getBatchSize()
                    && System.currentTimeMillis() < deadline) {
                Mqtt5Publish next = incoming.poll(10, TimeUnit.MILLISECONDS);
                if (Objects.isNull(next)) {
                    break;
                }
                publishes.add(next);
            }
            Acknowledgment ack = batchAck(publishes);
            int maxAttempts = Math.max(1, retryProperties.getMaxRetries()) + 1;
            for (int attempt = 1; ; attempt++) {
                try {
                    List<WireMqDelivery> deliveries = new ArrayList<>(publishes.size());
                    for (Mqtt5Publish publish : publishes) {
                        deliveries.add(deliveryOf(publish, ack));
                    }
                    batch.onBatch(deliveries);
                    return;
                } catch (MqPoisonException e) {
                    log.warn("MQTT poison batch dead-lettered, size={}", publishes.size(), e);
                    publishes.forEach(this::deadLetter);
                    return;
                } catch (Exception e) {
                    if (attempt >= maxAttempts) {
                        log.error("MQTT batch exhausted retries, dead-lettering, size={}",
                                publishes.size(), e);
                        publishes.forEach(this::deadLetter);
                        return;
                    }
                    sleepBackoff(attempt);
                }
            }
        }

        private void deliverSingle(Mqtt5Publish publish, int attempt) {
            Acknowledgment ack = new MqttAcknowledgment(publish, this, attempt);
            try {
                single.onDelivery(deliveryOf(publish, ack));
            } catch (MqPoisonException e) {
                deadLetter(publish);
            } catch (Exception e) {
                if (attempt >= Math.max(1, retryProperties.getMaxRetries()) + 1) {
                    log.error("MQTT delivery exhausted retries, dead-lettering, topic={}",
                            publish.getTopic(), e);
                    deadLetter(publish);
                } else {
                    sleepBackoff(attempt);
                    deliverSingle(publish, attempt + 1);
                }
            }
        }

        private Acknowledgment batchAck(List<Mqtt5Publish> publishes) {
            return new Acknowledgment() {
                @Override
                public void ack() {
                    publishes.forEach(Mqtt5Publish::acknowledge);
                }

                @Override
                public void reject(boolean requeue) {
                    if (requeue) {
                        publishes.forEach(incoming::add);
                    } else {
                        publishes.forEach(Dispatcher.this::deadLetter);
                    }
                }
            };
        }

        private void deadLetter(Mqtt5Publish publish) {
            try {
                publishClient.publish(Mqtt5Publish.builder()
                        .topic(deadLetterTopic(spec.topic()))
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .payload(publish.getPayload().orElse(null))
                        .userProperties(publish.getUserProperties())
                        .build()).join();
            } catch (Exception e) {
                log.error("MQTT dead-letter publish failed, topic={}", spec.topic(), e);
            }
            publish.acknowledge();
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
    }

    /**
     * ack acknowledges the publish (QoS 1); reject(true) re-enqueues for bounded
     * client-side redelivery (MQTT has no server nack); reject(false) dead-letters.
     */
    private record MqttAcknowledgment(Mqtt5Publish publish, Dispatcher dispatcher, int attempt)
            implements Acknowledgment {

        @Override
        public void ack() {
            publish.acknowledge();
        }

        @Override
        public void reject(boolean requeue) {
            if (requeue) {
                dispatcher.accept(publish);
                return;
            }
            dispatcher.deadLetter(publish);
        }
    }

    private record Subscription(Mqtt5AsyncClient client, Dispatcher dispatcher) {
    }

    private WireMqDelivery deliveryOf(Mqtt5Publish publish, Acknowledgment acknowledgment) {
        byte[] body = publish.getPayload().map(MqttMqAdapter::bytesOf).orElse(new byte[0]);
        return new WireMqDelivery(body, headersOf(publish), false, acknowledgment);
    }

    private static byte[] bytesOf(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private static Map<String, String> headersOf(Mqtt5Publish publish) {
        Map<String, String> headers = new HashMap<>();
        publish.getUserProperties().asList()
                .forEach(property -> headers.put(property.getName().toString(),
                        property.getValue().toString()));
        return headers;
    }

    private Mqtt5Publish publishOf(WireMqMessage wire) {
        com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder properties =
                com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties.builder();
        wire.headers().forEach((key, value) -> {
            if (Objects.nonNull(value)) {
                properties.add(key, value);
            }
        });
        return Mqtt5Publish.builder()
                .topic(topicName(wire.topic()))
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(ByteBuffer.wrap(wire.body()))
                .userProperties(properties.build())
                .build();
    }

    private Mqtt5Subscribe subscribeOf(SubscriptionSpec spec) {
        return Mqtt5Subscribe.builder()
                .topicFilter(filterOf(spec))
                .qos(MqttQos.AT_LEAST_ONCE)
                .build();
    }

    /**
     * LOAD_BALANCE rides a shared subscription (dead-letter destinations are plain —
     * they are point-to-point sinks); BROADCAST rides a plain filter.
     */
    private static String filterOf(SubscriptionSpec spec) {
        String filter = topicName(spec.topic());
        if (isDeadLetterTopic(spec.topic()) || spec.mode() == SubscriptionMode.BROADCAST) {
            return filter;
        }
        String share = spec.group().isBlank() ? DEFAULT_SHARE : spec.group();
        return "$share/" + share + "/" + filter;
    }

    private static boolean isDeadLetterTopic(MqTopic topic) {
        return topic == MqTopic.POINT_VALUE_DEAD || topic == MqTopic.POINT_COMMAND_DEAD
                || topic == MqTopic.COMMAND_DEAD;
    }

    private static String topicName(MqTopic topic) {
        return switch (topic) {
            case POINT_VALUE_DEAD -> "dc3/point_value/dlq";
            case POINT_COMMAND_DEAD -> "dc3/point_command/dlq";
            case COMMAND_DEAD -> "dc3/command/dlq";
            default -> "dc3/" + topic.name().toLowerCase();
        };
    }

    private static String deadLetterTopic(MqTopic topic) {
        return topicName(topic) + "/dlq";
    }
}
