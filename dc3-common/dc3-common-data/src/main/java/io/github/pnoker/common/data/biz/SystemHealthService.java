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

import io.github.pnoker.common.data.entity.vo.dashboard.SystemHealthVO;

/**
 * Aggregates liveness probes for the home-page banner: center services (auth / data /
 * manager), infrastructure (database / mq / gateway), and the driver fleet (total vs
 * online).
 *
 * @author pnoker
 * @since 2026.5.2
 */
public interface SystemHealthService {

    /**
     * Probe everything once and collect a snapshot. Probes must be short-circuited /
     * bounded in time so a slow dependency doesn't stall the dashboard.
     *
     * <p>
     * Takes tenantId because the driver / device fleet summaries query tenant-scoped
     * facades; infra / center probes ignore it.
     * </p>
     */
    SystemHealthVO snapshot(Long tenantId);

}
