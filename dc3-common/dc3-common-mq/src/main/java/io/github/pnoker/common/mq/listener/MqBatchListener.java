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

package io.github.pnoker.common.mq.listener;

import java.util.List;

/**
 * Batch listener for the highest-throughput paths (point values). The batch is the unit
 * of delivery and of acknowledgement: ack after the local transaction commits, reject
 * to requeue or dead-letter the batch as a whole. Bounded redelivery (RetryPolicy)
 * wraps failed invocations; exhaustion dead-letters rather than drops.
 *
 * @author pnoker
 * @since 2026.8.19
 */
@FunctionalInterface
public interface MqBatchListener<T> {

    /**
     * Handle one broker batch in broker delivery order.
     *
     * @param messages the received batch, never empty
     * @param ack      batch-level acknowledgement handle
     */
    void onBatch(List<MqReceived<T>> messages, Acknowledgment ack);
}
