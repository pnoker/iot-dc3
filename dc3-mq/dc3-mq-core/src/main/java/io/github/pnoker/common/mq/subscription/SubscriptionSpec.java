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

package io.github.pnoker.common.mq.subscription;

import io.github.pnoker.common.constant.mq.ConsumptionProfile;
import io.github.pnoker.common.constant.mq.DeliveryMode;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.constant.mq.SubscriptionMode;

import java.time.Duration;

/**
 * Subscription declaration — replaces {@code @RabbitListener} plus the container-factory
 * choice. Physical destinations are derived by the adapter from topic + mode + keyPattern.
 *
 * @param topic           logical destination
 * @param mode            load-balanced or broadcast
 * @param profile         latency/throughput tuning preset
 * @param delivery        single or batch delivery
 * @param keyPattern      subscription key filter relative to the topic (empty = topic
 *                        default), e.g. {@code "driver.*"} on STATE vs {@code "device.*"}
 * @param group           consumer group / per-instance queue suffix (drivers use their
 *                        client id); empty = platform-shared destination
 * @param instanceTtl     per-instance queue/subscription expiry, null = never expire
 * @param payloadType     type the listener expects
 * @param deadLetterEnabled whether rejects route to the topic's dead-letter
 * @author pnoker
 * @since 2026.8.19
 */
public record SubscriptionSpec(
        MqTopic topic,
        SubscriptionMode mode,
        ConsumptionProfile profile,
        DeliveryMode delivery,
        String keyPattern,
        String group,
        Duration instanceTtl,
        Class<?> payloadType,
        boolean deadLetterEnabled
) {

    public static SubscriptionSpec of(MqTopic topic, Class<?> payloadType) {
        return new SubscriptionSpec(topic, SubscriptionMode.LOAD_BALANCE, ConsumptionProfile.LATENCY,
                DeliveryMode.SINGLE, "", "", null, payloadType, true);
    }
}
