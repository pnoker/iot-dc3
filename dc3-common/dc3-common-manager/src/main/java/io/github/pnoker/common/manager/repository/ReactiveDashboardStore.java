package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.manager.entity.bo.dashboard.BucketRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.DailyGrowthRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.ProfileBindingRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyDeviceRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyDriverRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyPointRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyProfileRow;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Collection;

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
