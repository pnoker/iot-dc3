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

import java.util.List;

/**
 * Local SQLite-backed transactional outbox for point values delivered to RabbitMQ.
 *
 * <p>Readings are persisted before the first publish and removed only after RabbitMQ
 * confirms that the message was accepted and routed.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
public interface BufferService {

    /**
     * Initialize the SQLite database: create parent directories, open the connection
     * pool, validate WAL/FULL durability, and create the buffer table. Idempotent.
     */
    void initialize();

    /**
     * Persist and publish one point value using its message id as the outbox identity.
     *
     * @param pointValue point value with a stable message id
     * @param routingKey RabbitMQ routing key
     */
    void publish(PointValue pointValue, String routingKey);

    /**
     * Persist an entire group in one transaction before publishing any value.
     *
     * @param pointValues values with stable message ids
     * @param routingKey  RabbitMQ routing key
     */
    void publishBatch(List<PointValue> pointValues, String routingKey);

    /**
     * Republish due outbox records. A record is deleted only after a positive publisher
     * confirmation with no returned message.
     */
    void republishBatch();

    /**
     * @return current number of records awaiting republish
     */
    long pendingCount();
}
