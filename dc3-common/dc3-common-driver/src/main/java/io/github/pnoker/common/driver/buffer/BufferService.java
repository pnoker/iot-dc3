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

package io.github.pnoker.common.driver.buffer;

import io.github.pnoker.common.driver.entity.bean.PointValue;

/**
 * Local SQLite-backed buffer for point values that could not be delivered to RabbitMQ.
 *
 * <p>Failed/NACKed readings are persisted and republished by a Quartz job once the
 * broker recovers, so a RabbitMQ outage no longer loses collected data.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
public interface BufferService {

    /**
     * Initialize the SQLite database: create parent directories, open the connection
     * pool, and create the buffer table. Idempotent; a no-op when the buffer is disabled.
     */
    void initialize();

    /**
     * Persist a point value that failed to publish, keyed by the publisher-confirm
     * correlation id so a later NACK republish overwrites the same row (INSERT OR REPLACE).
     *
     * @param pointValue    the failed point value
     * @param routingKey    RabbitMQ routing key to republish with
     * @param correlationId publisher-confirm correlation id, used as the buffer row primary key
     * @param attempt       ordinal of the send attempt that just failed
     */
    void offer(PointValue pointValue, String routingKey, String correlationId, int attempt);

    /**
     * Republish up to {@code batchSize} due buffered point values, deleting the ones that
     * leave the channel cleanly and back-offing the ones that throw.
     */
    void republishBatch();

    /**
     * @return whether the buffer is enabled in configuration
     */
    boolean isEnabled();

    /**
     * @return current number of records awaiting republish (0 when disabled)
     */
    long pendingCount();
}
