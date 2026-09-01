package io.github.pnoker.common.data.biz;

import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import reactor.core.publisher.Mono;

import java.util.Map;

/** Reactive business service for tenant-scoped device status. */
public interface DeviceStatusService {

    Mono<Map<String, String>> list(FacadeDeviceOffsetQuery query);

    Mono<Map<String, String>> listByProfileId(Long tenantId, Long profileId);
}
