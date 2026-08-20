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

package io.github.pnoker.common.data.biz.store;

import io.github.pnoker.common.entity.bo.PointValueBO;

import java.util.List;

/**
 * Write orchestration of the telemetry path: lease-guard filtering, the
 * ingest idempotency window, the TSDB port append, and the relational
 * {@code dc3_point_latest} projection upsert — in that order.
 *
 * @author pnoker
 * @since 2026.8.20
 */
public interface PointValueIngestService {

    /**
     * Persist one value; single-value sugar over {@link #saveValues(List)}.
     *
     * @return {@code true} when the value was accepted (new, lease-valid)
     */
    boolean saveValue(PointValueBO valueBO);

    /**
     * Persist a batch. Values are dropped (not failed) when their message was
     * already persisted inside the idempotency window or when the sender is
     * not the device's active lease owner. Anything else — a store or
     * projection failure — propagates so the MQ consumer requeues the batch;
     * both writes are idempotent under replay.
     *
     * @return the accepted values in the original input order, deduplicated
     * by message id within the batch
     */
    List<PointValueBO> saveValues(List<PointValueBO> valueBOList);
}
