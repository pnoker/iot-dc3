package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.PointAttributeConfigFilter;
import io.github.pnoker.common.manager.repository.ReactivePointAttributeConfigStore;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactivePointAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactivePointAttributeService;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReactivePointAttributeConfigServiceImpl implements ReactivePointAttributeConfigService {
    private final ReactivePointAttributeConfigStore store;
    private final ReactivePointAttributeService attributeService;
    private final ReactiveDeviceService deviceService;
    private final ReactivePointService pointService;
    private final MetadataEventPublisher metadataEventPublisher;

    @Override public Mono<PointAttributeConfigBO> add(PointAttributeConfigBO value) {
        return validate(value, false)
                .then(Mono.defer(() -> store.getByAttributeDevicePoint(value.getTenantId(), value.getAttributeId(), value.getDeviceId(), value.getPointId())))
                .flatMap(existing -> Mono.<PointAttributeConfigBO>error(new DuplicateException("Point attribute config has been duplicated")))
                .switchIfEmpty(Mono.defer(() -> store.insert(normalize(value, false))))
                .onErrorMap(DataIntegrityViolationException.class, error -> new DuplicateException("Point attribute config has been duplicated"))
                .doOnNext(saved -> publish(saved.getTenantId(), saved.getDeviceId()));
    }

    @Override public Mono<PointAttributeConfigBO> update(PointAttributeConfigBO value) {
        return validate(value, true)
                .then(Mono.defer(() -> store.get(value.getTenantId(), value.getId())))
                .switchIfEmpty(Mono.error(new NotFoundException("Point attribute config does not exist")))
                .flatMap(current -> store.getByAttributeDevicePoint(value.getTenantId(), value.getAttributeId(), value.getDeviceId(), value.getPointId())
                        .filter(existing -> !Objects.equals(existing.getId(), value.getId()))
                        .flatMap(existing -> Mono.<PointAttributeConfigBO>error(new DuplicateException("Point attribute config has been duplicated")))
                        .switchIfEmpty(Mono.defer(() -> store.update(normalize(value, true), value.getVersion()))))
                .switchIfEmpty(Mono.error(new ConflictException("Point attribute config version conflict")))
                .doOnNext(saved -> publish(saved.getTenantId(), saved.getDeviceId()));
    }

    @Override public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        return store.get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Point attribute config does not exist")))
                .flatMap(value -> store.delete(tenantId, id, expectedVersion, operatorId, operatorName)
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(new ConflictException("Point attribute config version conflict")))
                        .doOnNext(ignored -> publish(value.getTenantId(), value.getDeviceId())));
    }

    @Override public Mono<PointAttributeConfigBO> getById(Long tenantId, Long id) {
        return store.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Point attribute config does not exist")));
    }

    @Override public Mono<PointAttributeConfigBO> getByAttributeIdAndDeviceIdAndPointId(Long tenantId, Long attributeId, Long deviceId, Long pointId) {
        return store.getByAttributeDevicePoint(tenantId, attributeId, deviceId, pointId).switchIfEmpty(Mono.error(new NotFoundException("Point attribute config does not exist")));
    }

    @Override public Flux<PointAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId) { return store.listByDeviceId(tenantId, deviceId); }
    @Override public Flux<PointAttributeConfigBO> listByDeviceIdAndPointId(Long tenantId, Long deviceId, Long pointId) { return store.listByDeviceIdAndPointId(tenantId, deviceId, pointId); }
    @Override public Mono<OffsetPage<PointAttributeConfigBO>> list(PointAttributeConfigFilter filter) { return store.list(filter); }

    private Mono<Void> validate(PointAttributeConfigBO value, boolean update) {
        if (value == null || !valid(value.getTenantId()) || !valid(value.getAttributeId()) || !valid(value.getDeviceId()) || !valid(value.getPointId())
                || (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0))) {
            return Mono.error(new RequestException("tenantId, attributeId, deviceId and pointId are required"));
        }
        return Mono.defer(() -> Mono.zip(attributeService.getById(value.getTenantId(), value.getAttributeId()), deviceService.getById(value.getTenantId(), value.getDeviceId()), pointService.getById(value.getTenantId(), value.getPointId())))
                .switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist")))
                .flatMap(tuple -> {
                    PointAttributeBO attribute = tuple.getT1();
                    DeviceBO device = tuple.getT2();
                    PointBO point = tuple.getT3();
                    if (!Objects.equals(attribute.getDriverId(), device.getDriverId()) || !Objects.equals(point.getProfileId(), device.getProfileId())) {
                        return Mono.error(new NotFoundException("Resource does not exist"));
                    }
                    return Mono.empty();
                });
    }

    private PointAttributeConfigBO normalize(PointAttributeConfigBO value, boolean update) {
        if (value.getEnableFlag() == null) value.setEnableFlag(io.github.pnoker.common.enums.EnableFlagEnum.ENABLE);
        if (!update && value.getVersion() == null) value.setVersion(0);
        return value;
    }

    private void publish(Long tenantId, Long deviceId) {
        if (tenantId != null && deviceId != null) metadataEventPublisher.publishEvent(new MetadataEvent(this, tenantId, deviceId, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE));
    }

    private boolean valid(Long value) { return value != null && value > 0; }
}
