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

public interface ReactiveDashboardStore {
    Flux<BucketRow> countDriverByEnable(Long tenantId);

    Flux<BucketRow> countDriverByType(Long tenantId);

    Flux<BucketRow> countDriverByService(Long tenantId);

    Flux<BucketRow> countDeviceByEnable(Long tenantId);

    Flux<BucketRow> countDeviceByDriver(Long tenantId, int limit);

    Flux<BucketRow> countDeviceByProfile(Long tenantId, int limit);

    Flux<DailyGrowthRow> dailyGrowth(Long tenantId, String table, LocalDateTime from, LocalDateTime to);

    Flux<TopologyDriverRow> topologyDrivers(Long tenantId);

    Flux<TopologyDeviceRow> topologyDevicesByDrivers(Long tenantId, Collection<Long> driverIds);

    Flux<ProfileBindingRow> topologyProfileBindings(Long tenantId, Collection<Long> deviceIds);

    Flux<TopologyProfileRow> topologyProfilesByIds(Long tenantId, Collection<Long> profileIds);

    Flux<TopologyPointRow> topologyPointsByProfiles(Long tenantId, Collection<Long> profileIds);
}
