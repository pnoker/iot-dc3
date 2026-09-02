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
package io.github.pnoker.common.mq.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.constant.common.RequestIdConstant;
import io.github.pnoker.common.constant.mq.DeliveryDisposition;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.constant.mq.OrderingGuarantee;
import io.github.pnoker.common.mq.MqHeaders;
import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.adapter.BrokerCapabilities;
import io.github.pnoker.common.mq.adapter.RawBatchListener;
import io.github.pnoker.common.mq.adapter.RawDeliveryListener;
import io.github.pnoker.common.mq.adapter.WireConfirmation;
import io.github.pnoker.common.mq.adapter.WireMqDelivery;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import io.github.pnoker.common.mq.message.WireMqMessage;
import io.github.pnoker.common.mq.subscription.SubscriptionSpec;
import io.github.pnoker.common.utils.JsonUtil;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.support.GenericApplicationContext;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class Dc3ListenerProcessorTest {

    @AfterEach
    void clearMdc() {
        MDC.remove(RequestIdConstant.MDC_KEY);
    }

    @Test
    void waitsForReactiveCompletionBeforeAck() {
        CompletionListener listener = new CompletionListener();
        CapturingAdapter adapter = register(listener);
        Mono<DeliveryDisposition> result = adapter.listener.onDelivery(delivery("value", Map.of()));

        StepVerifier.create(result)
                .expectSubscription()
                .then(() -> assertThat(listener.completed).isFalse())
                .then(listener.completion::tryEmitEmpty)
                .expectNext(DeliveryDisposition.ACK)
                .verifyComplete();

        assertThat(listener.completed).isTrue();
    }

    @Test
    void mapsExplicitRejectDecisions() {
        DecisionListener listener = new DecisionListener();
        CapturingAdapter adapter = register(listener);

        StepVerifier.create(adapter.listener.onDelivery(delivery("requeue", Map.of())))
                .expectNext(DeliveryDisposition.REQUEUE)
                .verifyComplete();
        StepVerifier.create(adapter.listener.onDelivery(delivery("dead", Map.of())))
                .expectNext(DeliveryDisposition.DEAD_LETTER)
                .verifyComplete();
    }

    @Test
    void propagatesPublisherErrorWithoutDecision() {
        CapturingAdapter adapter = register(new ErrorListener());

        StepVerifier.create(adapter.listener.onDelivery(delivery("value", Map.of())))
                .expectErrorMessage("listener failed")
                .verify();
    }

    @Test
    void rejectsConflictingDecisions() {
        CapturingAdapter adapter = register(new ConflictingListener());

        StepVerifier.create(adapter.listener.onDelivery(delivery("value", Map.of())))
                .expectErrorMatches(error -> error instanceof IllegalStateException
                        && error.getMessage().contains("Conflicting delivery dispositions"))
                .verify();
    }

    @Test
    void batchUsesOneCompletionDecision() {
        CapturingAdapter adapter = register(new BatchListener());

        StepVerifier.create(adapter.batchListener.onBatch(
                        List.of(delivery("first", Map.of()), delivery("second", Map.of()))))
                .expectNext(DeliveryDisposition.DEAD_LETTER)
                .verifyComplete();
    }

    @Test
    void cancellationDoesNotProduceDisposition() {
        CancellationListener listener = new CancellationListener();
        CapturingAdapter adapter = register(listener);

        StepVerifier.create(adapter.listener.onDelivery(delivery("value", Map.of())))
                .expectSubscription()
                .thenCancel()
                .verify();

        assertThat(listener.cancelled).isTrue();
    }

    @Test
    void requestIdMdcSurvivesAsyncBoundaryAndIsClearedAfterward() {
        MdcListener listener = new MdcListener();
        CapturingAdapter adapter = register(listener);

        StepVerifier.create(adapter.listener.onDelivery(delivery("value", Map.of(MqHeaders.REQUEST_ID, "request-42"))))
                .expectNext(DeliveryDisposition.ACK)
                .verifyComplete();

        assertThat(listener.requestId.get()).isEqualTo("request-42");
        assertThat(MDC.get(RequestIdConstant.MDC_KEY)).isNull();
    }

    private CapturingAdapter register(Object listener) {
        CapturingAdapter adapter = new CapturingAdapter();
        GenericApplicationContext context = new GenericApplicationContext();
        registerListenerBean(context, listener);
        context.refresh();
        Dc3ListenerProcessor processor = new Dc3ListenerProcessor(adapter);
        processor.setApplicationContext(context);
        processor.afterSingletonsInstantiated();
        context.close();
        return adapter;
    }

    @SuppressWarnings("unchecked")
    private void registerListenerBean(GenericApplicationContext context, Object listener) {
        context.registerBean("listener", (Class<Object>) listener.getClass(), () -> listener);
    }

    private WireMqDelivery delivery(String value, Map<String, String> headers) {
        return new WireMqDelivery(
                JsonUtil.toJsonString(new Payload(value)).getBytes(StandardCharsets.UTF_8), headers, false);
    }

    private record Payload(String value) {}

    private static final class CompletionListener {
        private final Sinks.Empty<Void> completion = Sinks.empty();
        private volatile boolean completed;

        @Dc3Listener(topic = MqTopic.STATE)
        Mono<Void> receive(MqReceived<Payload> message, Acknowledgment acknowledgment) {
            return completion.asMono().doOnSuccess(ignored -> completed = true);
        }
    }

    private static final class DecisionListener {
        @Dc3Listener(topic = MqTopic.STATE)
        Mono<Void> receive(MqReceived<Payload> message, Acknowledgment acknowledgment) {
            acknowledgment.reject("requeue".equals(message.payload().value()));
            return Mono.empty();
        }
    }

    private static final class ErrorListener {
        @Dc3Listener(topic = MqTopic.STATE)
        Mono<Void> receive(MqReceived<Payload> message, Acknowledgment acknowledgment) {
            return Mono.error(new IllegalStateException("listener failed"));
        }
    }

    private static final class ConflictingListener {
        @Dc3Listener(topic = MqTopic.STATE)
        Mono<Void> receive(MqReceived<Payload> message, Acknowledgment acknowledgment) {
            return Mono.fromRunnable(() -> {
                acknowledgment.ack();
                acknowledgment.reject(true);
            });
        }
    }

    private static final class BatchListener {
        @Dc3Listener(topic = MqTopic.POINT_VALUE)
        Mono<Void> receive(List<MqReceived<Payload>> messages, Acknowledgment acknowledgment) {
            acknowledgment.reject(false);
            return Mono.empty();
        }
    }

    private static final class CancellationListener {
        private final AtomicBoolean cancelled = new AtomicBoolean();

        @Dc3Listener(topic = MqTopic.STATE)
        Mono<Void> receive(MqReceived<Payload> message, Acknowledgment acknowledgment) {
            return Mono.<Void>never().doOnCancel(() -> cancelled.set(true));
        }
    }

    private static final class MdcListener {
        private final AtomicReference<String> requestId = new AtomicReference<>();

        @Dc3Listener(topic = MqTopic.STATE)
        Mono<Void> receive(MqReceived<Payload> message, Acknowledgment acknowledgment) {
            return Mono.delay(Duration.ofMillis(10), Schedulers.parallel())
                    .doOnNext(ignored -> requestId.set(MDC.get(RequestIdConstant.MDC_KEY)))
                    .then();
        }
    }

    private static final class CapturingAdapter implements BrokerAdapter {
        private RawDeliveryListener listener;
        private RawBatchListener batchListener;

        @Override
        public String type() {
            return "test";
        }

        @Override
        public BrokerCapabilities capabilities() {
            return new BrokerCapabilities(false, true, true, true, true, true, true, OrderingGuarantee.NONE);
        }

        @Override
        public void publish(WireMqMessage message) {}

        @Override
        public void publish(WireMqMessage message, WireConfirmation confirmation) {}

        @Override
        public void subscribe(SubscriptionSpec spec, RawDeliveryListener listener) {
            this.listener = listener;
        }

        @Override
        public void subscribeBatch(SubscriptionSpec spec, RawBatchListener listener) {
            this.batchListener = listener;
        }
    }
}
