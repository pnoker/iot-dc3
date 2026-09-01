package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import reactor.core.publisher.Mono;

/** Reactive metadata and ownership lookups required for command dispatch. */
public interface ReactivePointCommandContext {

    Mono<FacadeDeviceBO> device(Long tenantId, Long deviceId);

    Mono<FacadePointBO> point(Long tenantId, Long pointId);

    Mono<FacadeDriverBO> driverByDevice(Long tenantId, Long deviceId);

    Mono<FacadeDeviceOwnerBO> activeOwner(Long tenantId, Long deviceId);
}
