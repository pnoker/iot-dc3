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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 5 implementation of the broker port (EMQX / HiveMQ / NanoMQ / VerneMQ ...).
 * Topics map to {@code dc3/<topic>} (slash style); logical dead topics map to
 * {@code dc3/<topic>/dlq}. LOAD_BALANCE rides MQTT 5 shared subscriptions
 * ({@code $share/<group>/...}), BROADCAST rides a plain subscription — every
 * subscription gets its own client session, so broadcast instances are independent.
 * QoS 1 gives per-message ack and PUBACK publisher confirmation. MQTT has no server
 * nack: reject(true) is approximated by client-side bounded redelivery (the attempt
 * number rides the internal queue, so a permanently-rejecting listener is capped at
 * the batch path's attempt bound and dead-lettered, never looped); reject(false)
 * republishes to the {@code /dlq} topic and acknowledges only when the republish
 * succeeded. Delayed delivery and batch are the port fallback / synthesized
 * (capabilities false).
 *
 * <p>Key routing (MQTT has no binding-level key filter): the partition key rides the
 * wire as the {@code dc3-partition-key} user property, and LOAD_BALANCE specs sharing
 * a (topic, share group) share ONE client whose deliveries are routed by
 * {@link KeyRoutes} — a blank pattern matches everything, several matching listeners
 * round-robin, and a key matching no listener in this JVM is acknowledged and skipped
 * (Rabbit unroutable-drop semantics; the message's home, if any, is a matching
 * listener on another JVM). BROADCAST specs keep an independent client each.
 * {@code redelivered} reflects the internal attempt counter (attempt &gt; 1).
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

    /**
     * How long the HiveMQ callback waits for queue space before giving up on the
     * enqueue; a full wait would stall the client's netty threads, no wait would drop
     * QoS-1 messages under bursts.
     */
    private static final long ENQUEUE_TIMEOUT_MILLIS = 200;

    private final String host;
    private final int port;
    private final BatchConsumerProperties retryProperties;

    private final Mqtt5AsyncClient publishClient;
    private final Map<String, KeyRoutes<RawDeliveryListener>> singleRoutes = new ConcurrentHashMap<>();
    private final Map<String, KeyRoutes<RawBatchListener>> batchRoutes = new ConcurrentHashMap<>();
    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();
    private volatile boolean stopped;

    public MqttMqAdapter(String host, int port, BatchConsumerProperties retryProperties) {
        this.host = host;
        this.port = port;
        this.retryProperties = retryProperties;
        this.publishClient = client("dc3-mq-publisher-" + UUID.randomUUID());
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

    /**
     * The partition key for routing: MQTT has no native key field, so it rides the
     * wire as the {@code dc3-partition-key} user property.
     */
    private static String keyOf(Mqtt5Publish publish) {
        return headersOf(publish).get(MqHeaders.PARTITION_KEY);
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

    /**
     * Router key for one shared session: topic + share group + delivery mode; a
     * BROADCAST spec always gets its own per-instance key.
     */
    private static String routeKey(SubscriptionSpec spec) {
        String share = spec.mode() == SubscriptionMode.BROADCAST || isDeadLetterTopic(spec.topic())
                ? "broadcast-" + UUID.randomUUID()
                : (spec.group().isBlank() ? DEFAULT_SHARE : spec.group());
        return spec.topic() + "|" + share + "|" + spec.delivery();
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
        String routeKey = routeKey(spec);
        KeyRoutes<RawDeliveryListener> routes =
                singleRoutes.computeIfAbsent(routeKey, key -> new KeyRoutes<>());
        routes.add(spec.keyPattern(), listener);
        if (subscriptions.stream().anyMatch(subscription -> subscription.routeKey().equals(routeKey))) {
            log.info("MQTT subscription joined shared session, topic={}, mode={}, delivery={}, filter={}",
                    spec.topic(), spec.mode(), spec.delivery(), filterOf(spec));
            return;
        }
        Mqtt5AsyncClient client = client("dc3-mq-sub-" + UUID.randomUUID());
        Dispatcher dispatcher = new Dispatcher(spec, routes, null, routeKey);
        subscriptions.add(new Subscription(client, dispatcher, routeKey));
        client.subscribe(subscribeOf(spec), dispatcher::accept, true);
        dispatcher.start();
        log.info("MQTT subscription started, topic={}, mode={}, filter={}",
                spec.topic(), spec.mode(), filterOf(spec));
    }

    @Override
    public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
        String routeKey = routeKey(spec);
        KeyRoutes<RawBatchListener> routes = batchRoutes.computeIfAbsent(routeKey, key -> new KeyRoutes<>());
        routes.add(spec.keyPattern(), listener);
        if (subscriptions.stream().anyMatch(subscription -> subscription.routeKey().equals(routeKey))) {
            log.info("MQTT batch subscription joined shared session, topic={}, mode={}, delivery={}, filter={}",
                    spec.topic(), spec.mode(), spec.delivery(), filterOf(spec));
            return;
        }
        Mqtt5AsyncClient client = client("dc3-mq-sub-" + UUID.randomUUID());
        Dispatcher dispatcher = new Dispatcher(spec, null, routes, routeKey);
        subscriptions.add(new Subscription(client, dispatcher, routeKey));
        client.subscribe(subscribeOf(spec), dispatcher::accept, true);
        dispatcher.start();
        log.info("MQTT batch subscription started, topic={}, mode={}, filter={}",
                spec.topic(), spec.mode(), filterOf(spec));
    }

    /**
     * Disconnect every subscription session and the publisher. Idempotent.
     */
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        singleRoutes.clear();
        batchRoutes.clear();
        subscriptions.forEach(subscription -> {
            subscription.dispatcher().halt();
            subscription.client().disconnect();
        });
        subscriptions.clear();
        publishClient.disconnect();
    }

    private WireMqDelivery deliveryOf(Pending pending) {
        byte[] body = pending.publish().getPayload().map(MqttMqAdapter::bytesOf).orElse(new byte[0]);
        return new WireMqDelivery(body, headersOf(pending.publish()), pending.attempt() > 1);
    }

    private Mqtt5Publish publishOf(WireMqMessage wire) {
        com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder properties =
                com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties.builder();
        wire.headers().forEach((key, value) -> {
            if (Objects.nonNull(value)) {
                properties.add(key, value);
            }
        });
        if (Objects.nonNull(wire.partitionKey()) && !wire.partitionKey().isBlank()) {
            properties.add(MqHeaders.PARTITION_KEY, wire.partitionKey());
        }
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
     * A publish in flight through the internal queue together with its attempt number
     * (bounded client-side redelivery).
     */
    private record Pending(Mqtt5Publish publish, int attempt) {
    }

    /**
     * One broker session: the shared (or broadcast) client plus its dispatcher.
     */
    private record Subscription(Mqtt5AsyncClient client, Dispatcher dispatcher, String routeKey) {
    }

    /**
     * Per-session dispatch: single deliveries route each publish by key pattern with
     * bounded client-side redelivery for reject(true) — the attempt number rides the
     * internal queue so a permanently-rejecting listener dead-letters after the
     * bounded attempts instead of looping forever; batch subscriptions drain the
     * incoming queue into windowed, key-routed sub-batches with the shared synchronous
     * bounded-retry semantics. Poison and exhausted messages are republished to the
     * dead-letter topic and acknowledged only when the republish succeeded.
     */
    private final class Dispatcher implements Runnable {

        private final SubscriptionSpec spec;
        private final KeyRoutes<RawDeliveryListener> singleRoutesOfSession;
        private final KeyRoutes<RawBatchListener> batchRoutesOfSession;
        private final String routeKey;
        private final BlockingQueue<Pending> incoming = new ArrayBlockingQueue<>(10_000);
        private volatile boolean halted;
        private Thread thread;

        private Dispatcher(SubscriptionSpec spec, KeyRoutes<RawDeliveryListener> singleRoutesOfSession,
                           KeyRoutes<RawBatchListener> batchRoutesOfSession, String routeKey) {
            this.spec = spec;
            this.singleRoutesOfSession = singleRoutesOfSession;
            this.batchRoutesOfSession = batchRoutesOfSession;
            this.routeKey = routeKey;
        }

        void start() {
            thread = new Thread(this, "dc3-mq-mqtt-" + spec.topic());
            thread.setDaemon(true);
            thread.start();
        }

        void halt() {
            halted = true;
        }

        void accept(Mqtt5Publish publish) {
            try {
                // bounded wait instead of add(): add() throws IllegalStateException on a
                // full queue, which would drop the QoS-1 publish; on timeout the publish
                // is left UNACKNOWLEDGED so the broker redelivers it
                if (!incoming.offer(new Pending(publish, 1), ENQUEUE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                    log.error("MQTT dispatch queue full, leaving publish unacknowledged for broker redelivery, topic={}",
                            publish.getTopic());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("MQTT dispatch enqueue interrupted, leaving publish unacknowledged for broker redelivery, topic={}",
                        publish.getTopic(), e);
            }
        }

        @Override
        public void run() {
            while (!halted) {
                try {
                    if (Objects.nonNull(batchRoutesOfSession)) {
                        pumpBatch();
                    } else {
                        Pending pending = incoming.poll(200, TimeUnit.MILLISECONDS);
                        if (Objects.nonNull(pending)) {
                            deliverSingle(pending);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        private void pumpBatch() throws InterruptedException {
            Pending first = incoming.poll(200, TimeUnit.MILLISECONDS);
            if (Objects.isNull(first)) {
                return;
            }
            List<Pending> pendings = new ArrayList<>(List.of(first));
            long deadline = System.currentTimeMillis() + retryProperties.getReceiveTimeoutMillis();
            while (pendings.size() < retryProperties.getBatchSize()
                    && System.currentTimeMillis() < deadline) {
                Pending next = incoming.poll(10, TimeUnit.MILLISECONDS);
                if (Objects.isNull(next)) {
                    break;
                }
                pendings.add(next);
            }
            Map<RawBatchListener, List<Pending>> grouped = new LinkedHashMap<>();
            for (Pending pending : pendings) {
                RawBatchListener listener = batchRoutesOfSession.next(keyOf(pending.publish()));
                if (Objects.isNull(listener)) {
                    log.debug("MQTT batch message matched no listener in this JVM, acknowledging and skipping, topic={}, key={}",
                            spec.topic(), keyOf(pending.publish()));
                    pending.publish().acknowledge();
                    continue;
                }
                grouped.computeIfAbsent(listener, key -> new ArrayList<>()).add(pending);
            }
            for (Map.Entry<RawBatchListener, List<Pending>> entry : grouped.entrySet()) {
                deliverSubBatch(entry.getKey(), entry.getValue());
            }
        }

        private void deliverSubBatch(RawBatchListener listener, List<Pending> pendings) {
            normalize(Mono.defer(() -> listener.onBatch(pendings.stream().map(MqttMqAdapter.this::deliveryOf).toList())),
                    pendings.get(0).publish().getTopic().toString())
                    .flatMap(disposition -> settle(pendings, disposition))
                    .doOnError(error -> log.error("MQTT batch settlement failed, size={}", pendings.size(), error))
                    .subscribe();
        }

        private void deliverSingle(Pending pending) {
            RawDeliveryListener listener = singleRoutesOfSession.next(keyOf(pending.publish()));
            if (Objects.isNull(listener)) {
                log.debug("MQTT message matched no listener in this JVM, acknowledging and skipping, topic={}, key={}",
                        spec.topic(), keyOf(pending.publish()));
                pending.publish().acknowledge();
                return;
            }
            normalize(Mono.defer(() -> listener.onDelivery(deliveryOf(pending))), pending.publish().getTopic().toString())
                    .flatMap(disposition -> settle(List.of(pending), disposition))
                    .doOnError(error -> log.error("MQTT delivery settlement failed, topic={}",
                            pending.publish().getTopic(), error))
                    .subscribe();
        }

        private Mono<DeliveryDisposition> normalize(Mono<DeliveryDisposition> completion, String topic) {
            return completion
                    .onErrorResume(MqPoisonException.class, error -> {
                        log.warn("MQTT poison delivery dead-lettered, topic={}", topic, error);
                        return Mono.just(DeliveryDisposition.DEAD_LETTER);
                    })
                    .onErrorResume(error -> {
                        log.warn("MQTT delivery failed, requesting redelivery, topic={}", topic, error);
                        return Mono.just(DeliveryDisposition.REQUEUE);
                    });
        }

        private Mono<Void> settle(List<Pending> pendings, DeliveryDisposition disposition) {
            return switch (disposition) {
                case ACK -> Mono.fromRunnable(() ->
                        pendings.forEach(pending -> pending.publish().acknowledge()));
                case REQUEUE -> Mono.fromRunnable(() -> pendings.forEach(this::requeue));
                case DEAD_LETTER -> Flux.fromIterable(pendings)
                        .concatMap(pending -> deadLetter(pending.publish())).then();
            };
        }

        /**
         * Re-enqueue for another bounded client-side attempt (MQTT has no server
         * nack); past the attempt cap the publish is dead-lettered.
         */
        void requeue(Pending pending) {
            if (pending.attempt() >= maxAttempts()) {
                log.error("MQTT reject(requeue) exhausted attempts, dead-lettering, topic={}",
                        pending.publish().getTopic());
                deadLetter(pending.publish());
                return;
            }
            Pending next = new Pending(pending.publish(), pending.attempt() + 1);
            try {
                if (!incoming.offer(next, ENQUEUE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                    log.error("MQTT dispatch queue full on requeue, dead-lettering, topic={}",
                            pending.publish().getTopic());
                    deadLetter(pending.publish());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                deadLetter(pending.publish());
            }
        }

        private int maxAttempts() {
            return Math.max(1, retryProperties.getMaxRetries()) + 1;
        }

        /**
         * Republish to the dead-letter topic and acknowledge only when the republish
         * was accepted — a failed dead-letter publish leaves the message unacknowledged
         * so the broker redelivers it instead of losing it silently.
         */
        private Mono<Void> deadLetter(Mqtt5Publish publish) {
            return Mono.fromFuture(publishClient.publish(Mqtt5Publish.builder()
                        .topic(deadLetterTopic(spec.topic()))
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .payload(publish.getPayload().orElse(null))
                        .userProperties(publish.getUserProperties())
                        .build()))
                    .then(Mono.fromRunnable(publish::acknowledge));
        }
    }
}
