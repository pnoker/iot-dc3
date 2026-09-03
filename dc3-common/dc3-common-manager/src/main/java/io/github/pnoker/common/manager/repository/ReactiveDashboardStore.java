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
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.dashboard.BucketRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.DailyGrowthRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.ProfileBindingRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyDeviceRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyDriverRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyPointRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyProfileRow;
import java.time.LocalDateTime;
import java.util.Collection;
import reactor.core.publisher.Flux;
/** Reactive persistence port for dashboard records. */

public interface ReactiveDashboardStore {
    /** Emit driver counts grouped by enable flag. */
    Flux<BucketRow> countDriverByEnable(Long tenantId);

    /** Emit driver counts grouped by type. */
    Flux<BucketRow> countDriverByType(Long tenantId);

    /** Emit driver counts grouped by service. */
    Flux<BucketRow> countDriverByService(Long tenantId);

    /** Emit device counts grouped by enable flag. */
    Flux<BucketRow> countDeviceByEnable(Long tenantId);

    /** Emit the largest device counts grouped by driver. */
    Flux<BucketRow> countDeviceByDriver(Long tenantId, int limit);

    /** Emit the largest device counts grouped by profile. */
    Flux<BucketRow> countDeviceByProfile(Long tenantId, int limit);

    /** Emit daily row growth for the table inside the window. */
    Flux<DailyGrowthRow> dailyGrowth(Long tenantId, String table, LocalDateTime from, LocalDateTime to);

    /** Emit driver rows for the topology view. */
    Flux<TopologyDriverRow> topologyDrivers(Long tenantId);

    /** Emit device rows owned by the drivers for the topology view. */
    Flux<TopologyDeviceRow> topologyDevicesByDrivers(Long tenantId, Collection<Long> driverIds);

    /** Emit profile bindings for the devices. */
    Flux<ProfileBindingRow> topologyProfileBindings(Long tenantId, Collection<Long> deviceIds);

    /** Emit profile rows for the ids. */
    Flux<TopologyProfileRow> topologyProfilesByIds(Long tenantId, Collection<Long> profileIds);

    /** Emit point rows bound to the profiles. */
    Flux<TopologyPointRow> topologyPointsByProfiles(Long tenantId, Collection<Long> profileIds);
}
