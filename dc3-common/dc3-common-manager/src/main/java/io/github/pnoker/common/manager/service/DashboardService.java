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

/**
 * Manager-side dashboard aggregate service — powers the home page's
 * driver/device breakdown tabs.
 *
 * @author pnoker
 * @since 2026.5.2
 */
public interface DashboardService {

    DriverStatsVO driverStats(Long tenantId);

    DeviceStatsVO deviceStats(Long tenantId, int topN);

    /**
     * Daily new-row counts for driver / device / point / profile tables over
     * the last {@code days} days. Used to back the stat-card sparklines on
     * the home page. Arrays are fixed length = {@code days} with zero-padded
     * missing days, oldest first.
     */
    GrowthVO dailyGrowth(Long tenantId, int days);
}
