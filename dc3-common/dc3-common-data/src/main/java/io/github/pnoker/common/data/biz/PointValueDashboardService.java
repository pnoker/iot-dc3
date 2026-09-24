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
package io.github.pnoker.common.data.biz;

import io.github.pnoker.common.data.entity.vo.dashboard.PointValueDashboardVO;
import reactor.core.publisher.Mono;

/**
 * Data-dashboard analytics for a single point (位号看板). Serves everything the
 * point detail dashboard renders in one round trip: trend band, hourly volume,
 * value distribution, sampling-interval health, collection gaps and the typical
 * day curve.
 *
 * @author pnoker
 * @since 2026.9.22
 */
public interface PointValueDashboardService {

    /**
     * Build the dashboard payload for one point on one device of the tenant.
     *
     * @param tenantId   tenant scope, must be positive
     * @param deviceId   device scope, must belong to the tenant
     * @param pointId    point scope, must belong to the device profile
     * @param rangeHours lookback window in hours from 1 through 168, defaults to 24
     * @return the dashboard payload
     */
    Mono<PointValueDashboardVO> dashboard(Long tenantId, Long deviceId, Long pointId, Integer rangeHours);
}
