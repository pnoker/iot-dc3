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

package io.github.pnoker.common.mq;

import io.github.pnoker.common.constant.common.RequestIdConstant;

/**
 * Standardized envelope header names carried on every broker. The wire format is
 * identical across brokers: JSON body plus these string headers.
 *
 * @author pnoker
 * @since 2026.8.19
 */
public final class MqHeaders {

    /**
     * Payload class name; the API layer deserializes from it (replaces Spring AMQP's
     * {@code __TypeId__}, which adapters may still stamp for rolling-upgrade compatibility).
     */
    public static final String DC3_TYPE = "dc3-type";

    /**
     * Request id for trace continuity across the broker hop.
     */
    public static final String REQUEST_ID = RequestIdConstant.HEADER;

    /**
     * Tenant id mirrored for operational filtering; tenant scoping itself lives in the payload.
     */
    public static final String TENANT_ID = "tenant-id";

    /**
     * Business correlation id (command/record id). Adapters mirror it onto the broker's
     * native correlation property where one exists, so dead-letter consumers can trace
     * the originating record.
     */
    public static final String CORRELATION_ID = "dc3-correlation-id";

    /**
     * Partition key mirror for brokers with no native key field (MQTT user property).
     * Adapters whose broker carries the key natively (RabbitMQ routing key, Kafka
     * record key, Pulsar key) do not stamp it; the
     * adapters' client-side topic routers read whichever field their broker uses.
     */
    public static final String PARTITION_KEY = "dc3-partition-key";

    /**
     * Adapter-neutral delivery attempt counter. Brokers with no native redelivery
     * count increment this header when they republish a delivery for retry.
     */
    public static final String REDELIVERY_COUNT = "dc3-redelivery-count";

    private MqHeaders() {
        throw new IllegalStateException("Utility class");
    }
}
