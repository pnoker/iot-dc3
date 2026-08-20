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
 * Latest-value read service over the relational {@code dc3_point_latest}
 * projection. The projection deliberately stays out of the TSDB port: it is
 * OLTP state guarded by the fencing-token tuple, not time-series data
 * (docs/design/tsdb-abstraction.md §9.1).
 *
 * @author pnoker
 * @since 2026.8.20
 */
public interface PointValueLatestService {

    /**
     * Read the latest value of one point on one device.
     *
     * @return the value, or {@code null} when none exists
     */
    PointValueBO latest(Long tenantId, Long deviceId, Long pointId);

    /**
     * Read the latest value of each point of one device.
     *
     * @return the values (never {@code null}; missing points are omitted)
     */
    List<PointValueBO> listLatest(Long tenantId, Long deviceId, List<Long> pointIds);
}
