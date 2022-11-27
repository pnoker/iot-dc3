/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.constant;

import io.github.pnoker.common.constant.common.ExceptionConstant;

/**
 * 消息 相关常量
 *
 * @author pnoker
 * @since 2022.1.0
 */
public class RabbitConstant {

    private RabbitConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    // Arguments
    public static final String MESSAGE_TTL = "x-message-ttl";

    // Event
    public static final String TOPIC_EXCHANGE_EVENT = "dc3.exchange.event";
    public static final String ROUTING_DRIVER_EVENT_PREFIX = "dc3.routing.event.driver.";
    public static final String QUEUE_DRIVER_EVENT = "dc3.queue.event.driver";
    public static final String ROUTING_DEVICE_EVENT_PREFIX = "dc3.routing.event.device.";
    public static final String QUEUE_DEVICE_EVENT = "dc3.queue.event.device";

    // Metadata
    public static final String TOPIC_EXCHANGE_METADATA = "dc3.exchange.metadata";
    public static final String ROUTING_DRIVER_METADATA_PREFIX = "dc3.routing.metadata.driver.";
    public static final String QUEUE_DRIVER_METADATA_PREFIX = "dc3.queue.metadata.driver.";

    // Value
    public static final String TOPIC_EXCHANGE_VALUE = "dc3.exchange.value";
    public static final String ROUTING_POINT_VALUE_PREFIX = "dc3.routing.value.point.";
    public static final String QUEUE_POINT_VALUE = "dc3.queue.value.point";
}
