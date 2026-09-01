package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.PointAttributeFilter;
import io.github.pnoker.common.manager.repository.ReactivePointAttributeStore;
import io.github.pnoker.common.manager.service.ReactivePointAttributeService;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/** Default reactive point attribute application service. */
@Service
@RequiredArgsConstructor
public class ReactivePointAttributeServiceImpl implements ReactivePointAttributeService {
    private final ReactivePointAttributeStore store;
    private final ReactiveDriverService driverService;
    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public Mono<PointAttributeBO> add(PointAttributeBO value) {
        return Mono.defer(() -> {
            validate(value, false);
            return ensureDriver(value).then(store.getByCodeAndDriver(value.getTenantId(), value.getAttributeCode(), value.getDriverId()))
                    .flatMap(existing -> Mono.<PointAttributeBO>error(new DuplicateException("Command attribute has been duplicated")))
                    .switchIfEmpty(Mono.defer(() -> store.insert(normalize(value, false))))
                    .onErrorMap(DataIntegrityViolationException.class, error -> new DuplicateException("Command attribute has been duplicated"))
                    .doOnSuccess(saved -> publish(saved.getTenantId(), saved.getDriverId()));
        });
    }

    @Override
    public Mono<PointAttributeBO> update(PointAttributeBO value) {
        return Mono.defer(() -> {
            validate(value, true);
            return store.get(value.getTenantId(), value.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Command attribute does not exist")))
                    .flatMap(current -> ensureDriver(value).then(store.getByCodeAndDriver(value.getTenantId(), value.getAttributeCode(), value.getDriverId()))
                            .filter(existing -> !existing.getId().equals(value.getId()))
                            .flatMap(existing -> Mono.<PointAttributeBO>error(new DuplicateException("Command attribute has been duplicated")))
                            .switchIfEmpty(Mono.defer(() -> store.update(normalize(value, true), value.getVersion()))))
                    .switchIfEmpty(Mono.error(new ConflictException("Point attribute version conflict")))
                    .doOnSuccess(saved -> publish(saved.getTenantId(), saved.getDriverId()));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        return store.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Command attribute does not exist")))
                .flatMap(value -> store.delete(tenantId, id, expectedVersion, operatorId, operatorName)
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(new ConflictException("Point attribute version conflict")))
                        .doOnSuccess(ignored -> publish(value.getTenantId(), value.getDriverId())));
    }

    @Override
    public Mono<PointAttributeBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null) return Mono.error(new RequestException("Tenant ID and attribute ID are required"));
        return store.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Command attribute does not exist")));
    }

    @Override
    public Mono<PointAttributeBO> getByNameAndDriverId(Long tenantId, String name, Long driverId) {
        if (tenantId == null || driverId == null || name == null || name.isBlank()) return Mono.empty();
        return store.getByCodeAndDriver(tenantId, name, driverId);
    }

    @Override public Flux<PointAttributeBO> listByDriverId(Long tenantId, Long driverId) { return store.listByDriverId(tenantId, driverId); }
    @Override public Mono<OffsetPage<PointAttributeBO>> list(PointAttributeFilter filter) { return store.list(filter); }

    @Override
    public Mono<Boolean> deleteByIds(Long tenantId, Collection<Long> ids, Long operatorId, String operatorName) {
        return store.deleteByIds(tenantId, ids, operatorId, operatorName);
    }

    @Override
    public Mono<List<PointAttributeBO>> saveBatch(List<PointAttributeBO> values) {
        if (values == null || values.isEmpty()) return Mono.just(List.of());
        return Flux.fromIterable(values).concatMap(this::add).collectList();
    }

    @Override
    public Mono<List<PointAttributeBO>> updateBatch(List<PointAttributeBO> values) {
        if (values == null || values.isEmpty()) return Mono.just(List.of());
        return Flux.fromIterable(values).concatMap(this::update).collectList();
    }

    private Mono<Void> ensureDriver(PointAttributeBO value) {
        return driverService.getById(value.getTenantId(), value.getDriverId()).then();
    }

    private void validate(PointAttributeBO value, boolean update) {
        if (value == null || value.getTenantId() == null || value.getTenantId() <= 0
                || value.getDriverId() == null || value.getDriverId() <= 0
                || value.getAttributeName() == null || value.getAttributeName().isBlank()
                || value.getAttributeCode() == null || value.getAttributeCode().isBlank()) {
            throw new RequestException("Tenant ID, driver ID, attribute name and attribute code are required");
        }
        if (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0)) {
            throw new RequestException("Attribute ID and version are required for update");
        }
        value.setAttributeName(value.getAttributeName().trim());
        value.setAttributeCode(value.getAttributeCode().trim());
    }

    private PointAttributeBO normalize(PointAttributeBO value, boolean update) {
        if (value.getAttributeTypeFlag() == null) value.setAttributeTypeFlag(io.github.pnoker.common.enums.AttributeTypeEnum.STRING);
        if (value.getEnableFlag() == null) value.setEnableFlag(io.github.pnoker.common.enums.EnableFlagEnum.ENABLE);
        if (!update && value.getVersion() == null) value.setVersion(0);
        return value;
    }

    private void publish(Long tenantId, Long driverId) {
        if (tenantId != null && driverId != null) metadataEventPublisher.publishEvent(new MetadataEvent(this, tenantId, driverId,
                MetadataTypeEnum.DRIVER, MetadataOperateTypeEnum.UPDATE));
    }
}
