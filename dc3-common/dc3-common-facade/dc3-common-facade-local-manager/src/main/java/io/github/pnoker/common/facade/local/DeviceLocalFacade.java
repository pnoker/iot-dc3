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
package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceOwnerBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
import io.github.pnoker.common.facade.local.builder.FacadeDeviceBuilder;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.common.manager.repository.ReactiveDriverLeaseStore;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * In-process implementation: routes each call straight into {@link DeviceService}.
 * <p>
 * Selected when {@code dc3.facade.manager.mode=local}. Carries zero serialization cost — the same
 * JVM handles both caller and service.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
public class DeviceLocalFacade implements DeviceFacade {

    private final FacadeDeviceBuilder facadeDeviceBuilder;

    private final io.github.pnoker.common.manager.service.ReactiveDeviceService reactiveDeviceService;

    private final ReactiveDriverLeaseStore reactiveDriverLeaseStore;

    @org.springframework.beans.factory.annotation.Autowired
    public DeviceLocalFacade(
            FacadeDeviceBuilder facadeDeviceBuilder,
            io.github.pnoker.common.manager.service.ReactiveDeviceService reactiveDeviceService,
            ObjectProvider<ReactiveDriverLeaseStore> reactiveDriverLeaseStore) {
        this.facadeDeviceBuilder = facadeDeviceBuilder;
        this.reactiveDeviceService = reactiveDeviceService;
        this.reactiveDriverLeaseStore =
                reactiveDriverLeaseStore == null ? null : reactiveDriverLeaseStore.getIfAvailable();
    }

    @Override
    public Mono<FacadeDeviceBO> getByIdReactive(Long tenantId, Long id) {
        if (reactiveDeviceService == null)
            return Mono.error(new IllegalStateException("ReactiveDeviceService is not configured"));
        return reactiveDeviceService
                .getById(tenantId, id)
                .filter(device -> Objects.equals(tenantId, device.getTenantId()))
                .map(facadeDeviceBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadeDeviceBO> listByIdsReactive(Long tenantId, Collection<Long> ids) {
        if (reactiveDeviceService == null)
            return Flux.error(new IllegalStateException("ReactiveDeviceService is not configured"));
        List<Long> normalized = ids == null
                ? List.of()
                : ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        return reactiveDeviceService.listByIds(tenantId, normalized).map(facadeDeviceBuilder::toFacadeBO);
    }

    @Override
    public Mono<OffsetPage<FacadeDeviceBO>> listReactive(FacadeDeviceOffsetQuery query) {
        if (reactiveDeviceService == null)
            return Mono.error(new IllegalStateException("ReactiveDeviceService is not configured"));
        DeviceFilter filter = new DeviceFilter(
                query.tenantId(),
                query.deviceName(),
                query.deviceCode(),
                query.driverId(),
                query.profileId(),
                query.enableFlag(),
                query.version(),
                query.groupId(),
                query.labelId(),
                query.offset(),
                query.limit(),
                query.sort());
        return reactiveDeviceService
                .list(filter)
                .map(page -> OffsetPage.of(
                        page.items().stream()
                                .map(facadeDeviceBuilder::toFacadeBO)
                                .toList(),
                        page.offset(),
                        page.limit(),
                        page.total()));
    }

    @Override
    public Flux<FacadeDeviceBO> listByProfileIdReactive(Long tenantId, Long profileId) {
        if (reactiveDeviceService == null)
            return Flux.error(new IllegalStateException("ReactiveDeviceService is not configured"));
        return reactiveDeviceService.listByProfileId(tenantId, profileId).map(facadeDeviceBuilder::toFacadeBO);
    }

    @Override
    public Flux<FacadeDeviceBO> listByDriverIdReactive(Long tenantId, Long driverId) {
        if (reactiveDeviceService == null)
            return Flux.error(new IllegalStateException("ReactiveDeviceService is not configured"));
        return reactiveDeviceService.listByDriverId(tenantId, driverId).map(facadeDeviceBuilder::toFacadeBO);
    }

    @Override
    public Mono<FacadeDeviceOwnerBO> getActiveOwnerReactive(Long tenantId, Long deviceId) {
        if (reactiveDriverLeaseStore == null)
            return Mono.error(new IllegalStateException("ReactiveDriverLeaseStore is not configured"));
        return reactiveDriverLeaseStore
                .getActiveLease(tenantId, deviceId)
                .map(owner ->
                        new FacadeDeviceOwnerBO(owner.getDriverId(), owner.getOwnerNode(), owner.getFencingToken()));
    }
}
