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

package io.github.pnoker.common.mq.rocketmq;

import io.github.pnoker.common.constant.mq.ConsumptionProfile;
import io.github.pnoker.common.constant.mq.DeliveryMode;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.constant.mq.SubscriptionMode;
import io.github.pnoker.common.mq.adapter.WireMqDelivery;
import io.github.pnoker.common.mq.config.BatchConsumerProperties;
import io.github.pnoker.common.mq.core.EnvelopeCodec;
import io.github.pnoker.common.mq.core.MessageSenderImpl;
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.subscription.SubscriptionSpec;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Isolation probe for the classic-client fresh-consumer-group behavior that blocks
 * rocketmq certification: does a brand-new consumer group on an existing topic see
 * messages published before it subscribed? Runs only when TCK_ROCKETMQ_NAMESRV points
 * at a live name server.
 *
 * @author pnoker
 * @since 2026.8.19
 */
class RocketMqFreshGroupProbe {

    private static final String NAMESRV = System.getenv("TCK_ROCKETMQ_NAMESRV");

    private RocketMqAdapter adapter;
    private MessageSenderImpl sender;

    @BeforeEach
    void requireBroker() {
        Assumptions.assumeTrue(Objects.nonNull(NAMESRV), "set TCK_ROCKETMQ_NAMESRV");
        BatchConsumerProperties properties = new BatchConsumerProperties();
        properties.setBatchSize(10);
        properties.setMaxRetries(2);
        properties.setRetryInitialIntervalMillis(100);
        properties.setRetryMultiplier(2);
        properties.setRetryMaxIntervalMillis(200);
        adapter = new RocketMqAdapter(NAMESRV, properties);
        sender = new MessageSenderImpl(adapter);
    }

    private List<String> subscribe(String group) {
        List<String> received = new CopyOnWriteArrayList<>();
        adapter.subscribe(new SubscriptionSpec(MqTopic.EVENT, SubscriptionMode.LOAD_BALANCE,
                ConsumptionProfile.LATENCY, DeliveryMode.SINGLE, "", group, null, String.class, true),
                delivery -> {
                    received.add(new String(delivery.body(), java.nio.charset.StandardCharsets.UTF_8));
                    delivery.acknowledgment().ack();
                });
        settle();
        return received;
    }

    private static void settle() {
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void freshGroupSkipsBacklogPublishedBeforeSubscription() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        // group A subscribes first, so the topic exists before phase two
        List<String> groupA = subscribe("probe-a-" + run);
        for (int i = 0; i < 10; i++) {
            sender.send(MqMessage.of(MqTopic.EVENT, "probe", "backlog-" + i));
        }
        await("group A consumed the backlog", () -> groupA.size() >= 10);

        // brand-new group subscribing AFTER the backlog was consumed/published
        List<String> groupB = subscribe("probe-b-" + run);
        settle();
        sender.send(MqMessage.of(MqTopic.EVENT, "probe", "fresh-1"));

        await("group B got its own message", () -> groupB.stream().anyMatch(m -> m.contains("fresh-1")));
        System.err.println("[PROBE] groupB received: " + groupB);
        assertThat(groupB).noneMatch(m -> m.contains("backlog"));
    }

    @Test
    void freshGroupOnExistingTopicSkipsPreexistingBacklog() {
        String run = UUID.randomUUID().toString().substring(0, 8);

        // publish WITHOUT any subscriber, so the topic exists with a backlog
        for (int i = 0; i < 10; i++) {
            sender.send(MqMessage.of(MqTopic.METADATA, "probe", "pre-" + i));
        }
        settle();

        List<String> late = new CopyOnWriteArrayList<>();
        adapter.subscribe(new SubscriptionSpec(MqTopic.METADATA, SubscriptionMode.LOAD_BALANCE,
                        ConsumptionProfile.LATENCY, DeliveryMode.SINGLE, "probe", "probe-late-" + run, null,
                        String.class, true),
                delivery -> {
                    late.add(new String(delivery.body(), java.nio.charset.StandardCharsets.UTF_8));
                    delivery.acknowledgment().ack();
                });
        settle();
        sender.send(MqMessage.of(MqTopic.METADATA, "probe", "late-1"));

        await("late group got its own message", () -> late.stream().anyMatch(m -> m.contains("late-1")));
        System.err.println("[PROBE] late group received: " + late);
        assertThat(late).noneMatch(m -> m.contains("pre-"));
    }

    private static void await(String what, java.util.function.BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            settle();
        }
        throw new IllegalStateException("timeout waiting for: " + what);
    }
}
