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

package io.github.pnoker.common.mq.tck;

import io.github.pnoker.common.constant.mq.ConsumptionProfile;
import io.github.pnoker.common.constant.mq.DeliveryMode;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.constant.mq.SubscriptionMode;
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.adapter.RawBatchListener;
import io.github.pnoker.common.mq.adapter.RawDeliveryListener;
import io.github.pnoker.common.mq.adapter.WireMqDelivery;
import io.github.pnoker.common.mq.core.EnvelopeCodec;
import io.github.pnoker.common.mq.core.MessageSenderImpl;
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.sender.MessageSender;
import io.github.pnoker.common.mq.subscription.SubscriptionSpec;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Broker-neutral contract suite (docs/design/mq-abstraction.md §11). A broker adapter
 * passes this suite ⇒ it is compliant. Community adapters depend on this module and
 * provide a harness that instantiates their {@link BrokerAdapter} against a live broker.
 * Cases use unique groups and payload markers so a fresh broker per run keeps them
 * isolated.
 *
 * @author pnoker
 * @since 2026.8.19
 */
public abstract class AbstractMqContractTest {

    /**
     * Simple wire payload with a unique marker per message.
     */
    public record TestPayload(String id, String text) {
    }

    /**
     * Collector pairing received payloads with their headers.
     */
    protected static final class Received {
        final TestPayload payload;
        final Map<String, String> headers;
        final boolean redelivered;

        Received(WireMqDelivery delivery) {
            this.payload = EnvelopeCodec.deserialize(delivery, TestPayload.class);
            this.headers = delivery.headers();
            this.redelivered = delivery.redelivered();
        }
    }

    /**
     * Unique suffix per suite run so repeated runs against a long-lived broker never
     * observe each other's messages.
     */
    protected static final String RUN = UUID.randomUUID().toString().substring(0, 8);

    /**
     * The adapter under test, wired against the suite's disposable broker.
     *
     * @return the adapter harness provided by the concrete test
     */
    protected abstract BrokerAdapter adapter();

    /**
     * Hook to release broker resources between cases; the default is a no-op.
     */
    protected void shutdownAdapter() {
    }

    @AfterEach
    void tearDown() {
        shutdownAdapter();
        MDC.remove(io.github.pnoker.common.constant.common.RequestIdConstant.MDC_KEY);
    }

    /**
     * Sender bound to the adapter under test.
     *
     * @return a fresh sender instance
     */
    protected final MessageSender sender() {
        return new MessageSenderImpl(adapter());
    }

    /**
     * Load-balanced single-delivery subscription on a run-unique group.
     */
    protected final SubscriptionSpec loadBalance(MqTopic topic, String group) {
        return new SubscriptionSpec(topic, SubscriptionMode.LOAD_BALANCE, ConsumptionProfile.LATENCY,
                DeliveryMode.SINGLE, "", group + "-" + RUN, null, TestPayload.class, true);
    }

    /**
     * Load-balanced subscription with a key filter and no per-instance TTL.
     */
    protected final SubscriptionSpec loadBalancePattern(MqTopic topic, String group, String keyPattern) {
        return loadBalancePattern(topic, group, keyPattern, null);
    }

    /**
     * Load-balanced subscription with a key filter and an optional per-instance TTL.
     */
    protected final SubscriptionSpec loadBalancePattern(MqTopic topic, String group, String keyPattern,
                                                        java.time.Duration instanceTtl) {
        return new SubscriptionSpec(topic, SubscriptionMode.LOAD_BALANCE, ConsumptionProfile.LATENCY,
                DeliveryMode.SINGLE, keyPattern, group + "-" + RUN, instanceTtl, TestPayload.class, true);
    }

    /**
     * Broadcast single-delivery subscription with a run-unique group.
     */
    protected final SubscriptionSpec broadcast(MqTopic topic, String group) {
        return new SubscriptionSpec(topic, SubscriptionMode.BROADCAST, ConsumptionProfile.LATENCY,
                DeliveryMode.SINGLE, "tckbroadcast", group + "-" + RUN, null, TestPayload.class, true);
    }

    /**
     * Throughput-profile batch subscription on a run-unique group.
     */
    protected final SubscriptionSpec batchSpec(MqTopic topic) {
        return new SubscriptionSpec(topic, SubscriptionMode.LOAD_BALANCE, ConsumptionProfile.THROUGHPUT,
                DeliveryMode.BATCH, "", "tck-batch-" + RUN, null, TestPayload.class, true);
    }

    /**
     * Subscribe and collect received deliveries. When {@code each} is null, deliveries are
     * acknowledged immediately and recorded; otherwise the callback owns the acknowledgment.
     */
    protected final List<Received> subscribeCollector(SubscriptionSpec spec, Consumer<WireMqDelivery> each) {
        List<Received> received = new CopyOnWriteArrayList<>();
        adapter().subscribe(spec, delivery -> {
            if (Objects.nonNull(each)) {
                each.accept(delivery);
            } else {
                received.add(new Received(delivery));
                delivery.acknowledgment().ack();
            }
        });
        settle();
        return received;
    }

    /**
     * Batch subscribe and collect received deliveries. When {@code batch} is null, the
     * first delivery's acknowledgment is used to ack the batch; otherwise the callback owns it.
     */
    protected final List<Received> subscribeBatchCollector(SubscriptionSpec spec,
                                                           java.util.function.BiConsumer<List<Received>,
                                                                   io.github.pnoker.common.mq.listener.Acknowledgment> batch) {
        List<Received> received = new CopyOnWriteArrayList<>();
        adapter().subscribeBatch(spec, deliveries -> {
            List<Received> batchReceived = deliveries.stream().map(Received::new).toList();
            received.addAll(batchReceived);
            if (Objects.nonNull(batch)) {
                batch.accept(batchReceived, deliveries.get(0).acknowledgment());
            } else {
                deliveries.get(0).acknowledgment().ack();
            }
        });
        settle();
        return received;
    }

    /**
     * Consumer-group join latency allowance: log-based brokers (kafka) with
     * auto.offset.reset=latest only see messages published after the group has joined.
     */
    protected final void settle() {
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Uniquely identified payload for correlation across broker round-trips.
     */
    protected final TestPayload payload(String text) {
        return new TestPayload(UUID.randomUUID().toString(), text);
    }

    /**
     * Awaitility shorthand with the suite's default 15s timeout.
     */
    protected final void await(String description, java.util.function.BooleanSupplier condition) {
        Awaitility.await(description).atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(100))
                .until(condition::getAsBoolean);
    }

    @Test
    void roundTripPreservesEnvelopeHeadersAndPayload() {
        List<Received> received = subscribeCollector(loadBalance(MqTopic.EVENT, "tck-rt"), null);
        TestPayload sent = payload("round-trip");

        MDC.put(io.github.pnoker.common.constant.common.RequestIdConstant.MDC_KEY, "tck-request-1");
        sender().send(MqMessage.of(MqTopic.EVENT, "tckrt", sent));

        await("round-trip delivery", () -> !received.isEmpty());
        assertThat(received.get(0).payload).isEqualTo(sent);
        assertThat(received.get(0).headers).containsEntry(MqHeaders.DC3_TYPE, TestPayload.class.getName());
        assertThat(received.get(0).headers).containsEntry(MqHeaders.REQUEST_ID, "tck-request-1");
    }

    @Test
    void loadBalanceDeliversEachMessageExactlyOnceAcrossInstances() {
        List<Received> first = subscribeCollector(loadBalance(MqTopic.EVENT, "tck-lb"), null);
        List<Received> second = subscribeCollector(loadBalance(MqTopic.EVENT, "tck-lb"), null);

        MessageSender sender = sender();
        for (int i = 0; i < 6; i++) {
            sender.send(MqMessage.of(MqTopic.EVENT, "tcklb", payload("lb-" + i)));
        }

        await("all six messages consumed", () -> first.size() + second.size() >= 6);
        Set<String> ids = ConcurrentHashMap.newKeySet();
        first.forEach(r -> ids.add(r.payload.id()));
        second.forEach(r -> ids.add(r.payload.id()));
        assertThat(ids).hasSize(6);
    }

    @Test
    void broadcastDeliversToEveryInstance() {
        List<Received> first = subscribeCollector(broadcast(MqTopic.METADATA, "tck-bc-a"), null);
        List<Received> second = subscribeCollector(broadcast(MqTopic.METADATA, "tck-bc-b"), null);

        MessageSender sender = sender();
        for (int i = 0; i < 3; i++) {
            sender.send(MqMessage.of(MqTopic.METADATA, "tckbroadcast", payload("bc-" + i)));
        }

        await("first instance saw all", () -> first.size() >= 3);
        await("second instance saw all", () -> second.size() >= 3);
    }

    @Test
    void delayIsRespectedThroughTheFallback() {
        List<Received> received = subscribeCollector(loadBalance(MqTopic.EVENT, "tck-delay"), null);

        long start = System.nanoTime();
        sender().send(MqMessage.builder()
                .topic(MqTopic.EVENT)
                .partitionKey("tckdelay")
                .payload(payload("delayed"))
                .delay(Duration.ofSeconds(2))
                .build());

        assertThat(received).isEmpty();
        await("delayed delivery", () -> !received.isEmpty());
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(1500);
    }

    @Test
    void rejectWithoutRequeueRoutesToTheDeadLetter() {
        List<Received> dead = subscribeCollector(loadBalance(MqTopic.POINT_COMMAND_DEAD, "tck-dlq-reader"), null);
        subscribeCollector(loadBalancePattern(MqTopic.POINT_COMMAND, "tck-dlq", "tck.*"),
                delivery -> delivery.acknowledgment().reject(false));

        TestPayload sent = payload("doomed");
        sender().send(MqMessage.of(MqTopic.POINT_COMMAND, "tck.node", sent));

        await("message reaches the dead letter", () -> dead.stream().anyMatch(r -> r.payload.equals(sent)));
    }

    @Test
    void rejectWithRequeueRedelivers() {
        AtomicInteger attempts = new AtomicInteger();
        subscribeCollector(loadBalancePattern(MqTopic.POINT_COMMAND, "tck-rq", "tck.*"), delivery -> {
            if (attempts.getAndIncrement() == 0) {
                delivery.acknowledgment().reject(true);
            } else {
                delivery.acknowledgment().ack();
            }
        });

        sender().send(MqMessage.of(MqTopic.POINT_COMMAND, "tck.node", payload("retry")));

        await("redelivery observed", () -> attempts.get() >= 2);
    }

    @Test
    void sendAsyncConfirmationFires() throws Exception {
        subscribeCollector(loadBalance(MqTopic.EVENT, "tck-confirm"), null);
        CountDownLatch confirmed = new CountDownLatch(1);
        AtomicInteger outcome = new AtomicInteger(-1);

        sender().sendAsync(MqMessage.of(MqTopic.EVENT, "tckconfirm", payload("confirmed")),
                (message, ok, cause) -> {
                    outcome.set(ok ? 1 : 0);
                    confirmed.countDown();
                });

        assertThat(confirmed.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(outcome.get()).isEqualTo(1);
    }

    @Test
    void burstOfMessagesIsNotLost() {
        List<Received> received = subscribeCollector(loadBalance(MqTopic.EVENT, "tck-burst"), null);

        MessageSender sender = sender();
        for (int i = 0; i < 100; i++) {
            sender.send(MqMessage.of(MqTopic.EVENT, "tckburst", payload("burst-" + i)));
        }

        await("all 100 burst messages delivered", () -> received.size() >= 100);
    }

    @Test
    void batchDeliveryCommitsTheWholeBatch() {
        List<Received> received = subscribeBatchCollector(batchSpec(MqTopic.POINT_VALUE), null);

        MessageSender sender = sender();
        for (int i = 0; i < 10; i++) {
            sender.send(MqMessage.of(MqTopic.POINT_VALUE, "tckbatch", payload("batch-" + i)));
        }

        await("all batch messages delivered", () -> received.size() >= 10);
        int afterAck = received.size();
        Awaitility.await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(2))
                .until(() -> received.size() == afterAck);
    }

    @Test
    void retryExhaustionDeadLettersInsteadOfDropping() {
        List<Received> dead = subscribeCollector(loadBalance(MqTopic.POINT_VALUE_DEAD, "tck-dead-reader"), null);
        subscribeBatchCollector(batchSpec(MqTopic.POINT_VALUE), (batch, ack) -> {
            throw new IllegalStateException("always failing listener");
        });

        TestPayload sent = payload("exhausted");
        sender().send(MqMessage.of(MqTopic.POINT_VALUE, "tckbatch", sent));

        await("exhausted retries dead-letter the message",
                () -> dead.stream().anyMatch(r -> r.payload.equals(sent)));
    }

    @Test
    void messagesSurviveWhileNoConsumerIsRunning() {
        List<Received> first = subscribeCollector(loadBalancePattern(MqTopic.COMMAND, "tck-durable", "tck.*"), null);
        shutdownAdapter();

        MessageSender sender = sender();
        for (int i = 0; i < 3; i++) {
            sender.send(MqMessage.of(MqTopic.COMMAND, "tck.node", payload("durable-" + i)));
        }

        List<Received> second = subscribeCollector(loadBalancePattern(MqTopic.COMMAND, "tck-durable", "tck.*"), null);
        await("durable messages redelivered after restart", () -> second.size() >= 3);
    }

    /**
     * Per-instance subscription expiry is broker-specific; harnesses without the
     * capability override this test with a disabled assumption.
     */
    @Test
    public abstract void perInstanceSubscriptionExpiresAfterInstanceStops();
}
