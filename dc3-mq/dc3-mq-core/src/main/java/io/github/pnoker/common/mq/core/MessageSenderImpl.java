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

import io.github.pnoker.common.mq.adapter.BrokerAdapter;
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.message.WireMqMessage;
import io.github.pnoker.common.mq.sender.MessageSender;
import io.github.pnoker.common.mq.sender.MqPublishException;
import io.github.pnoker.common.mq.sender.SendConfirmation;
import io.github.pnoker.common.mq.sender.ReactiveMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Core {@link MessageSender} implementation: serializes, stamps headers, negotiates the
 * delay capability against the active adapter and applies the local-scheduler fallback
 * for brokers without native delayed delivery.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Slf4j
@RequiredArgsConstructor
public class MessageSenderImpl implements MessageSender, ReactiveMessageSender {

    private static final ScheduledExecutorService DELAY_FALLBACK = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable, "dc3-mq-delay-fallback");
        thread.setDaemon(true);
        return thread;
    });

    private final BrokerAdapter adapter;

    @Override
    public void send(MqMessage message) {
        WireMqMessage wire = EnvelopeCodec.prepare(message);
        publishWithDelayFallback(message, wire);
    }

    @Override
    public void sendAsync(MqMessage message, SendConfirmation confirmation) {
        WireMqMessage wire = EnvelopeCodec.prepare(message);
        adapter.publish(wire, (envelope, confirmed, cause) ->
                confirmation.onConfirm(message, confirmed, cause));
    }

    @Override
    public void sendConfirmed(MqMessage message, Duration timeout) {
        WireMqMessage wire = EnvelopeCodec.prepare(message);
        if (!adapter.capabilities().publisherConfirm()) {
            log.debug("Broker has no publisher confirmation, degrading sendConfirmed to plain send, topic={}",
                    message.getTopic());
            adapter.publish(wire);
            return;
        }
        CompletableFuture<Boolean> routed = new CompletableFuture<>();
        CompletableFuture<Throwable> failure = new CompletableFuture<>();
        adapter.publish(wire, (envelope, confirmed, cause) -> {
            if (confirmed) {
                routed.complete(true);
            } else {
                failure.complete(cause != null ? cause
                        : new MqPublishException("Broker did not confirm the publish"));
            }
        });
        try {
            routed.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new MqPublishException("MQ publisher confirm timed out after " + timeout.toMillis() + "ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MqPublishException("Interrupted while waiting for MQ publisher confirm", e);
        } catch (Exception e) {
            Throwable cause = failure.isDone() ? failure.join() : e;
            throw new MqPublishException("MQ publish was not confirmed: " + cause.getMessage(), cause);
        }
    }

    @Override
    public reactor.core.publisher.Mono<Void> sendConfirmed(MqMessage message) {
        return reactor.core.publisher.Mono.create(sink -> {
            AtomicBoolean cancelled = new AtomicBoolean();
            sink.onCancel(() -> cancelled.set(true));
            WireMqMessage wire;
            try {
                wire = EnvelopeCodec.prepare(message);
            } catch (Throwable error) {
                sink.error(error);
                return;
            }
            adapter.publish(wire, (envelope, confirmed, cause) -> {
                if (cancelled.get()) {
                    return;
                }
                if (confirmed) {
                    sink.success();
                } else {
                    sink.error(cause != null ? cause : new MqPublishException("Broker did not confirm the publish"));
                }
            });
        });
    }

    private void publishWithDelayFallback(MqMessage message, WireMqMessage wire) {
        boolean delayed = !wire.delay().isZero();
        if (!delayed || adapter.capabilities().delayedMessage()) {
            adapter.publish(wire);
            return;
        }
        log.info("Broker has no native delayed delivery, scheduling locally, topic={}, delayMillis={}",
                message.getTopic(), wire.delay().toMillis());
        DELAY_FALLBACK.schedule(() -> {
            try {
                adapter.publish(new WireMqMessage(wire.topic(), wire.partitionKey(), wire.body(),
                        wire.headers(), Duration.ZERO));
            } catch (Exception e) {
                log.error("Delayed fallback re-send failed, topic={}", wire.topic(), e);
            }
        }, wire.delay().toMillis(), TimeUnit.MILLISECONDS);
    }
}
