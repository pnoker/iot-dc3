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

package io.github.pnoker.common.mq.annotation;

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.constant.mq.ConsumptionProfile;
import io.github.pnoker.common.constant.mq.DeliveryMode;
import io.github.pnoker.common.constant.mq.SubscriptionMode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a broker-neutral message listener. Supported method shapes:
 *
 * <pre>{@code
 * @Dc3Listener(topic = MqTopic.STATE, keyPattern = "driver.*")
 * public void onState(MqReceived<DriverStateDTO> message, Acknowledgment ack) { ... }
 *
 * @Dc3Listener(topic = MqTopic.POINT_VALUE, profile = THROUGHPUT, delivery = BATCH)
 * public void onValues(List<MqReceived<PointValueBO>> messages, Acknowledgment ack) { ... }
 * }</pre>
 *
 * Batch size, prefetch and the retry policy bind from configuration
 * ({@code dc3.data.point.batch.*}), not annotation literals.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Dc3Listener {

    /**
     * @return logical destination
     */
    MqTopic topic();

    /**
     * @return load-balanced or broadcast subscription
     */
    SubscriptionMode mode() default SubscriptionMode.LOAD_BALANCE;

    /**
     * @return latency/throughput tuning preset
     */
    ConsumptionProfile profile() default ConsumptionProfile.LATENCY;

    /**
     * @return single or batch delivery
     */
    DeliveryMode delivery() default DeliveryMode.SINGLE;

    /**
     * @return subscription key filter relative to the topic, empty for the topic default
     */
    String keyPattern() default "";

    /**
     * @return consumer group / per-instance queue suffix, empty for the platform-shared
     *         destination (drivers set their client id programmatically)
     */
    String group() default "";

    /**
     * @return whether rejects route to the topic's dead-letter
     */
    boolean deadLetter() default true;
}
