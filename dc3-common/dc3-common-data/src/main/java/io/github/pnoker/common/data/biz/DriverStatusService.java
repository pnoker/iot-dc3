package io.github.pnoker.common.data.biz;

import io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery;
import reactor.core.publisher.Mono;

import java.util.Map;

/** Reactive business service for tenant-scoped driver status. */
public interface DriverStatusService {

    Mono<Map<String, String>> list(FacadeDriverOffsetQuery query);

    Mono<Long> countOnlineDevices(Long tenantId, Long driverId);

    Mono<Long> countOfflineDevices(Long tenantId, Long driverId);
}
