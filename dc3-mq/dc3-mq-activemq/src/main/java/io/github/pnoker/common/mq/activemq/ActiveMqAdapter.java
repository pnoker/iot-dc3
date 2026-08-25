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

package io.github.pnoker.common.mq.activemq;

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
import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ActiveMQ (Artemis / Classic) implementation of the broker port over JMS 2.0.
 *
 * <p>Live topics map to JMS topics: LOAD_BALANCE rides a <b>shared durable
 * subscription</b> named after the consumer group (competing consumers share it,
 * offline messages are retained), BROADCAST rides a plain topic consumer per
 * instance — one publish fans out to every subscription, matching the rabbit
 * exchange semantics. Dead-letter destinations are queues. Delays use JMS scheduled
 * delivery natively; rejecting without requeue republishes to the
 * {@code dc3.<topic>.dlq} queue; batches are synthesized by draining the consumer
 * within a short window (capability false). JMS has no publisher confirmation — the
 * synchronous persistent send returning is what the adapter reports (best-effort).
 *
 * <p>Key routing (JMS has no binding-level key filter): the partition key rides the
 * wire as the {@code dc3-partition-key} string property, and LOAD_BALANCE specs
 * sharing a (topic, subscription) share ONE consumer whose deliveries are routed by
 * {@link KeyRoutes} — a blank pattern matches everything, several matching listeners
 * round-robin, and a key matching no listener in this JVM is acknowledged and skipped
 * (Rabbit unroutable-drop semantics; the message's home, if any, is a matching
 * listener on another JVM). Specs on the same (topic, subscription) must agree on the
 * delivery mode; BROADCAST specs keep an independent consumer each.
 *
 * <p>JMS property names must be valid java identifiers, so the dashed standard
 * headers ({@code dc3-type}, {@code X-Request-Id}, {@code dc3-correlation-id}) ride
 * the wire underscored and are restored on read.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
public class ActiveMqAdapter implements BrokerAdapter {

    /**
     * Default shared-durable subscription name when the spec carries no group.
     */
    private static final String DEFAULT_SUBSCRIPTION = "dc3-mq";

    private final ConnectionFactory connectionFactory;
    private final BatchConsumerProperties retryProperties;

    private final JMSContext publishContext;
    private final Map<String, KeyRoutes<RawDeliveryListener>> singleRoutes = new ConcurrentHashMap<>();
    private final Map<String, KeyRoutes<RawBatchListener>> batchRoutes = new ConcurrentHashMap<>();
    private final List<Connection> subscriptions = new CopyOnWriteArrayList<>();
    private final List<BatchPump> pumps = new CopyOnWriteArrayList<>();
    private volatile boolean stopped;

    public ActiveMqAdapter(ConnectionFactory connectionFactory, BatchConsumerProperties retryProperties) {
        this.connectionFactory = connectionFactory;
        this.retryProperties = retryProperties;
        this.publishContext = connectionFactory.createContext(JMSContext.CLIENT_ACKNOWLEDGE);
    }

    @Override
    public String type() {
        return "activemq";
    }

    @Override
    public BrokerCapabilities capabilities() {
        return new BrokerCapabilities(true, false, true, true, false, false, false, OrderingGuarantee.NONE);
    }

    @Override
    public void publish(WireMqMessage message) {
        publish(message, null);
    }

    @Override
    public void publish(WireMqMessage message, WireConfirmation confirmation) {
        try {
            synchronized (publishContext) {
                JMSProducer producer = publishContext.createProducer();
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                if (!message.delay().isZero()) {
                    producer.setDeliveryDelay(message.delay().toMillis());
                }
                producer.send(destinationOf(message.topic()), jmsMessage(publishContext, message));
            }
            if (Objects.nonNull(confirmation)) {
                confirmation.onConfirm(message, true, null);
            }
        } catch (JMSException e) {
            if (Objects.nonNull(confirmation)) {
                confirmation.onConfirm(message, false, e);
                return;
            }
            throw new IllegalStateException("ActiveMQ publish failed, topic=" + message.topic(), e);
        }
    }

    @Override
    public void subscribe(SubscriptionSpec spec, RawDeliveryListener listener) {
        try {
            String routeKey = routeKey(spec);
            KeyRoutes<RawDeliveryListener> routes =
                    singleRoutes.computeIfAbsent(routeKey, key -> new KeyRoutes<>());
            routes.add(spec.keyPattern(), listener);
            if (singleRoutes.get(routeKey).size() > 1) {
                // an existing connection already feeds the router for this subscription
                log.info("ActiveMQ subscription joined shared consumer, topic={}, mode={}, destination={}",
                        spec.topic(), spec.mode(), destinationName(spec.topic()) + subscriptionSuffix(spec));
                return;
            }
            Connection connection = connectionFactory.createConnection();
            subscriptions.add(connection);
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            MessageConsumer consumer = consumerOf(spec, session);
            String destinationLabel = destinationName(spec.topic()) + subscriptionSuffix(spec);
            consumer.setMessageListener(message -> {
                Acknowledgment ack = new ActiveMqAcknowledgment(message, session, spec.topic(), this);
                deliverSingle(message, spec.topic(), routes, ack, session, destinationLabel);
            });
            connection.start();
            log.info("ActiveMQ subscription started, topic={}, mode={}, destination={}",
                    spec.topic(), spec.mode(), destinationLabel);
        } catch (JMSException e) {
            throw new IllegalStateException("ActiveMQ subscribe failed, topic=" + spec.topic(), e);
        }
    }

    @Override
    public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
        try {
            String routeKey = routeKey(spec);
            KeyRoutes<RawBatchListener> routes = batchRoutes.computeIfAbsent(routeKey, key -> new KeyRoutes<>());
            routes.add(spec.keyPattern(), listener);
            if (batchRoutes.get(routeKey).size() > 1) {
                log.info("ActiveMQ batch subscription joined shared pump, topic={}, destination={}",
                        spec.topic(), destinationName(spec.topic()) + subscriptionSuffix(spec));
                return;
            }
            Connection connection = connectionFactory.createConnection();
            subscriptions.add(connection);
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            MessageConsumer consumer = consumerOf(spec, session);
            BatchPump pump = new BatchPump(spec, consumer, session, routes);
            pumps.add(pump);
            Thread thread = new Thread(pump, "dc3-mq-activemq-batch-" + spec.topic());
            thread.setDaemon(true);
            thread.start();
            connection.start();
            log.info("ActiveMQ batch subscription started, topic={}, destination={}",
                    spec.topic(), destinationName(spec.topic()) + subscriptionSuffix(spec));
        } catch (JMSException e) {
            throw new IllegalStateException("ActiveMQ subscribeBatch failed, topic=" + spec.topic(), e);
        }
    }

    private void deliverSingle(Message message, MqTopic topic, KeyRoutes<RawDeliveryListener> routes,
                               Acknowledgment ack, Session session, String destinationLabel) {
        RawDeliveryListener listener = routes.next(keyOf(message));
        if (Objects.isNull(listener)) {
            log.debug("ActiveMQ message matched no listener in this JVM, acknowledging and skipping, topic={}, key={}",
                    topic, keyOf(message));
            ack.ack();
            return;
        }
        try {
            listener.onDelivery(deliveryOf(message, ack));
        } catch (MqPoisonException e) {
            deadLetter(message, topic);
        } catch (Exception e) {
            log.warn("ActiveMQ delivery failed, recovering session for redelivery, destination={}",
                    destinationLabel, e);
            recover(session);
        }
    }

    /**
     * Stop every subscription connection and batch pump this adapter started.
     * Idempotent. The shared publish context stays open on purpose: publishing
     * must survive stop() (the durability contract publishes while no consumer
     * runs), and the broker retains those messages for the next subscriber.
     * Shared durable subscriptions survive offline — that is their point.
     */
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        singleRoutes.clear();
        batchRoutes.clear();
        pumps.forEach(BatchPump::halt);
        subscriptions.forEach(connection -> {
            try {
                connection.close();
            } catch (JMSException e) {
                log.debug("ActiveMQ connection close failed", e);
            }
        });
        subscriptions.clear();
    }

    private void recover(Session session) {
        try {
            session.recover();
        } catch (JMSException e) {
            log.warn("ActiveMQ session recover failed", e);
        }
    }

    private void deadLetter(Message message, MqTopic topic) {
        try {
            synchronized (publishContext) {
                JMSProducer producer = publishContext.createProducer();
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                producer.send(publishContext.createQueue(deadLetterQueue(topic)), message);
            }
            message.acknowledge();
        } catch (JMSException e) {
            log.warn("ActiveMQ dead-letter publish failed, topic={}", topic, e);
        }
    }

    /**
     * Synthesized batch consumer: block for the first message, drain up to batchSize
     * within the receive window, then route the drained messages to the listener whose
     * pattern matches each key and deliver one sub-batch per listener with the shared
     * synchronous bounded-retry semantics; exhaustion dead-letters and acknowledges.
     */
    private final class BatchPump implements Runnable {

        private final SubscriptionSpec spec;
        private final MessageConsumer consumer;
        private final Session session;
        private final KeyRoutes<RawBatchListener> routes;
        private volatile boolean halted;

        private BatchPump(SubscriptionSpec spec, MessageConsumer consumer, Session session,
                          KeyRoutes<RawBatchListener> routes) {
            this.spec = spec;
            this.consumer = consumer;
            this.session = session;
            this.routes = routes;
        }

        void halt() {
            halted = true;
        }

        @Override
        public void run() {
            while (!halted) {
                try {
                    Message first = consumer.receive(200);
                    if (Objects.isNull(first)) {
                        continue;
                    }
                    List<Message> batch = new ArrayList<>(List.of(first));
                    long deadline = System.currentTimeMillis() + retryProperties.getReceiveTimeoutMillis();
                    while (batch.size() < retryProperties.getBatchSize()
                            && System.currentTimeMillis() < deadline) {
                        Message next = consumer.receive(10);
                        if (Objects.isNull(next)) {
                            break;
                        }
                        batch.add(next);
                    }
                    deliverWithRetry(batch);
                } catch (JMSException e) {
                    if (!halted) {
                        log.warn("ActiveMQ batch pump receive failed, topic={}", spec.topic(), e);
                    }
                }
            }
        }

        private void deliverWithRetry(List<Message> batch) {
            Map<RawBatchListener, List<Message>> grouped = new LinkedHashMap<>();
            for (Message message : batch) {
                RawBatchListener listener = routes.next(keyOf(message));
                if (Objects.isNull(listener)) {
                    log.debug("ActiveMQ batch message matched no listener in this JVM, skipping, topic={}, key={}",
                            spec.topic(), keyOf(message));
                    continue;
                }
                grouped.computeIfAbsent(listener, key -> new ArrayList<>()).add(message);
            }
            for (Map.Entry<RawBatchListener, List<Message>> entry : grouped.entrySet()) {
                deliverSubBatch(entry.getKey(), entry.getValue());
            }
        }

        private void deliverSubBatch(RawBatchListener listener, List<Message> subBatch) {
            Message last = subBatch.get(subBatch.size() - 1);
            Acknowledgment ack = new ActiveMqAcknowledgment(last, session, spec.topic(), ActiveMqAdapter.this);
            int maxAttempts = Math.max(1, retryProperties.getMaxRetries()) + 1;
            for (int attempt = 1; ; attempt++) {
                try {
                    List<WireMqDelivery> deliveries = new ArrayList<>(subBatch.size());
                    for (Message message : subBatch) {
                        deliveries.add(deliveryOf(message, ack));
                    }
                    listener.onBatch(deliveries);
                    return;
                } catch (MqPoisonException e) {
                    log.warn("ActiveMQ poison batch dead-lettered, size={}", subBatch.size(), e);
                    subBatch.forEach(message -> deadLetter(message, spec.topic()));
                    return;
                } catch (Exception e) {
                    if (attempt >= maxAttempts) {
                        log.error("ActiveMQ batch exhausted retries, dead-lettering, size={}", subBatch.size(), e);
                        subBatch.forEach(message -> deadLetter(message, spec.topic()));
                        return;
                    }
                    sleepBackoff(attempt);
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
    }

    /**
     * ack acknowledges the session's consumed messages (batch-granular by design);
     * reject(true) recovers the session for redelivery; reject(false) dead-letters the
     * delivery's topic then acknowledges everything consumed.
     */
    private record ActiveMqAcknowledgment(Message message, Session session, MqTopic topic,
                                          ActiveMqAdapter adapter) implements Acknowledgment {

        @Override
        public void ack() {
            try {
                message.acknowledge();
            } catch (JMSException e) {
                log.warn("ActiveMQ acknowledge failed", e);
            }
        }

        @Override
        public void reject(boolean requeue) {
            if (requeue) {
                adapter.recover(session);
                return;
            }
            adapter.deadLetter(message, topic);
        }
    }

    /**
     * Consumer for a subscription: dead-letter destinations are queues; live topics are
     * JMS topics where LOAD_BALANCE rides a shared durable subscription named after the
     * consumer group and BROADCAST rides a plain per-instance consumer.
     */
    private static MessageConsumer consumerOf(SubscriptionSpec spec, Session session) throws JMSException {
        if (isDeadLetterTopic(spec.topic())) {
            return session.createConsumer(session.createQueue(destinationName(spec.topic())));
        }
        Topic topic = session.createTopic(destinationName(spec.topic()));
        if (spec.mode() == SubscriptionMode.BROADCAST) {
            return session.createConsumer(topic);
        }
        String subscription = spec.group().isBlank() ? DEFAULT_SUBSCRIPTION : spec.group();
        return session.createSharedDurableConsumer(topic, subscription);
    }

    private static String subscriptionSuffix(SubscriptionSpec spec) {
        if (spec.mode() != SubscriptionMode.LOAD_BALANCE || spec.group().isBlank()) {
            return "";
        }
        return " (" + spec.group() + ")";
    }

    /**
     * Router key for one shared consumer: topic + subscription + delivery mode; a
     * BROADCAST spec always gets its own per-instance key (plain consumer, no shared
     * durable subscription).
     */
    private static String routeKey(SubscriptionSpec spec) {
        String subscription = spec.mode() == SubscriptionMode.BROADCAST
                ? "broadcast-" + UUID.randomUUID()
                : (spec.group().isBlank() ? DEFAULT_SUBSCRIPTION : spec.group());
        return spec.topic() + "|" + subscription + "|" + spec.delivery();
    }

    /**
     * The partition key rides the wire as the {@code dc3-partition-key} JMS property.
     */
    private static String keyOf(Message message) {
        try {
            return message.getStringProperty(jmsHeaderName(MqHeaders.PARTITION_KEY));
        } catch (JMSException e) {
            return null;
        }
    }

    private WireMqDelivery deliveryOf(Message message, Acknowledgment acknowledgment) {
        return new WireMqDelivery(bodyOf(message), headersOf(message), redeliveredOf(message), acknowledgment);
    }

    private static boolean redeliveredOf(Message message) {
        try {
            return message.getJMSRedelivered();
        } catch (JMSException e) {
            return false;
        }
    }

    private static byte[] bodyOf(Message message) {
        try {
            if (message instanceof BytesMessage bytes) {
                byte[] body = new byte[(int) bytes.getBodyLength()];
                bytes.readBytes(body);
                return body;
            }
            return new byte[0];
        } catch (JMSException e) {
            return new byte[0];
        }
    }

    private static Map<String, String> headersOf(Message message) {
        Map<String, String> headers = new HashMap<>();
        try {
            Enumeration<String> names = message.getPropertyNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                Object value = message.getObjectProperty(name);
                headers.put(portHeaderName(name), Objects.isNull(value) ? null : String.valueOf(value));
            }
        } catch (JMSException e) {
            log.debug("ActiveMQ header read failed", e);
        }
        return headers;
    }

    private Message jmsMessage(JMSContext context, WireMqMessage wire) throws JMSException {
        BytesMessage message = context.createBytesMessage();
        message.writeBytes(wire.body());
        for (Map.Entry<String, String> header : wire.headers().entrySet()) {
            if (Objects.nonNull(header.getValue())) {
                message.setStringProperty(jmsHeaderName(header.getKey()), header.getValue());
            }
        }
        if (Objects.nonNull(wire.partitionKey()) && !wire.partitionKey().isBlank()) {
            // JMS has no native key field: mirror the partition key as a property for
            // the client-side topic router
            message.setStringProperty(jmsHeaderName(MqHeaders.PARTITION_KEY), wire.partitionKey());
        }
        return message;
    }

    /**
     * JMS property names must be valid java identifiers; the standard envelope headers
     * use dashes, so they ride the wire underscored.
     */
    private static String jmsHeaderName(String name) {
        return name.replace('-', '_');
    }

    private static String portHeaderName(String jmsName) {
        return switch (jmsName) {
            case "dc3_type" -> "dc3-type";
            case "X_Request_Id" -> "X-Request-Id";
            case "dc3_correlation_id" -> "dc3-correlation-id";
            case "tenant_id" -> "tenant-id";
            case "dc3_partition_key" -> "dc3-partition-key";
            default -> jmsName;
        };
    }

    private Destination destinationOf(MqTopic topic) {
        if (isDeadLetterTopic(topic)) {
            return publishContext.createQueue(destinationName(topic));
        }
        return publishContext.createTopic(destinationName(topic));
    }

    private static boolean isDeadLetterTopic(MqTopic topic) {
        return topic == MqTopic.POINT_VALUE_DEAD || topic == MqTopic.POINT_COMMAND_DEAD
                || topic == MqTopic.COMMAND_DEAD;
    }

    private static String destinationName(MqTopic topic) {
        return switch (topic) {
            case POINT_VALUE_DEAD -> "dc3.point_value.dlq";
            case POINT_COMMAND_DEAD -> "dc3.point_command.dlq";
            case COMMAND_DEAD -> "dc3.command.dlq";
            default -> "dc3." + topic.name().toLowerCase();
        };
    }

    private static String deadLetterQueue(MqTopic topic) {
        return destinationName(topic) + ".dlq";
    }
}
