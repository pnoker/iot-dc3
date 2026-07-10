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

package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.manager.entity.vo.dashboard.DeviceStatsVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.DriverStatsVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.GrowthVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.TopologyVO;

/**
 * Manager-side dashboard aggregate service — powers the home page's driver/device
 * breakdown tabs.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
public interface DashboardService {

    /**
     * Aggregate driver statistics for the dashboard home view: driver counts grouped by
     * enable status and protocol type.
     *
     * @param tenantId caller's tenant id
     * @return driver statistics for the tenant
     */
    DriverStatsVO driverStats(Long tenantId);

    /**
     * Aggregate device statistics for the dashboard home view: device counts plus the
     * top-N most active devices by point-value volume.
     *
     * @param tenantId caller's tenant id
     * @param topN     number of top devices to return, ranked by point-value volume
     * @return device statistics for the tenant
     */
    DeviceStatsVO deviceStats(Long tenantId, int topN);

    /**
     * Daily new-row counts for driver / device / point / profile tables over the last
     * {@code days} days. Used to back the stat-card sparklines on the home page. Arrays
     * are fixed length = {@code days} with zero-padded missing days, oldest first.
     */
    GrowthVO dailyGrowth(Long tenantId, int days);

    /**
     * Four-column topology Sankey (Driver → Device → Profile → Point) used by the home
     * page. Top-N cropped server-side; overflow rolls into {@code others:*} nodes whose
     * {@code hiddenChildren} carry the collapsed list for drill-in.
     *
     * <p>
     * {@code mode="cardinality"} weights edges by the number of relationships (1 per
     * device / binding / point). {@code mode="volume"} weights edges by the number of
     * point_value samples each carried over the {@code rangeKey} window (today / 24h / 7d
     * / 30d); entity Top-N cropping at each layer also sorts by those volumes.
     * </p>
     *
     * <p>
     * Results are cached for 60s keyed by {@code tenant:mode:rangeKey} — the graph
     * changes slowly and the volume-mode aggregate touches the Timescale hypertable view,
     * so repeated refreshes stay cheap.
     * </p>
     *
     * @param tenantId caller's tenant id
     * @param mode     {@code "cardinality"} | {@code "volume"}
     * @param rangeKey time window key for volume mode; ignored for cardinality. Defaults
     *                 to {@code "7d"} when blank.
     */
    TopologyVO topology(Long tenantId, String mode, String rangeKey);

}
