package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.repository.EventAttributeConfigFilter;
import io.github.pnoker.common.manager.repository.ReactiveEventAttributeConfigStore;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactiveEventAttributeConfigService;
import io.github.pnoker.common.manager.service.ReactiveEventAttributeService;
import io.github.pnoker.common.manager.service.ReactiveEventService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReactiveEventAttributeConfigServiceImpl implements ReactiveEventAttributeConfigService {
    private final ReactiveEventAttributeConfigStore store;
    private final ReactiveEventAttributeService attributeService;
    private final ReactiveDeviceService deviceService;
    private final ReactiveEventService eventService;
    private final MetadataEventPublisher metadataEventPublisher;
    @Override public Mono<EventAttributeConfigBO> add(EventAttributeConfigBO value) { return validate(value, false).then(Mono.defer(() -> store.getByAttributeDeviceEvent(value.getTenantId(), value.getAttributeId(), value.getDeviceId(), value.getEventId()))).flatMap(existing -> Mono.<EventAttributeConfigBO>error(new DuplicateException("Event attribute config has been duplicated"))).switchIfEmpty(Mono.defer(() -> store.insert(normalize(value, false)))).onErrorMap(DataIntegrityViolationException.class, error -> new DuplicateException("Event attribute config has been duplicated")).doOnNext(saved -> publish(saved.getTenantId(), saved.getDeviceId())); }
    @Override public Mono<EventAttributeConfigBO> update(EventAttributeConfigBO value) { return validate(value, true).then(Mono.defer(() -> store.get(value.getTenantId(), value.getId()))).switchIfEmpty(Mono.error(new NotFoundException("Event attribute config does not exist"))).flatMap(current -> store.getByAttributeDeviceEvent(value.getTenantId(), value.getAttributeId(), value.getDeviceId(), value.getEventId()).filter(existing -> !Objects.equals(existing.getId(), value.getId())).flatMap(existing -> Mono.<EventAttributeConfigBO>error(new DuplicateException("Event attribute config has been duplicated"))).switchIfEmpty(Mono.defer(() -> store.update(normalize(value, true), value.getVersion())))).switchIfEmpty(Mono.error(new ConflictException("Event attribute config version conflict"))).doOnNext(saved -> publish(saved.getTenantId(), saved.getDeviceId())); }
    @Override public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) { return store.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Event attribute config does not exist"))).flatMap(value -> store.delete(tenantId, id, expectedVersion, operatorId, operatorName).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(new ConflictException("Event attribute config version conflict"))).doOnNext(ignored -> publish(value.getTenantId(), value.getDeviceId()))); }
    @Override public Mono<EventAttributeConfigBO> getById(Long tenantId, Long id) { return store.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Event attribute config does not exist"))); }
    @Override public Mono<EventAttributeConfigBO> getByAttributeIdAndDeviceIdAndEventId(Long tenantId, Long attributeId, Long deviceId, Long eventId) { return store.getByAttributeDeviceEvent(tenantId, attributeId, deviceId, eventId).switchIfEmpty(Mono.error(new NotFoundException("Event attribute config does not exist"))); }
    @Override public Flux<EventAttributeConfigBO> listByDeviceId(Long tenantId, Long deviceId) { return store.listByDeviceId(tenantId, deviceId); }
    @Override public Flux<EventAttributeConfigBO> listByDeviceIdAndEventId(Long tenantId, Long deviceId, Long eventId) { return store.listByDeviceIdAndEventId(tenantId, deviceId, eventId); }
    @Override public Mono<OffsetPage<EventAttributeConfigBO>> list(EventAttributeConfigFilter filter) { return store.list(filter); }
    private Mono<Void> validate(EventAttributeConfigBO value, boolean update) { if (value == null || !valid(value.getTenantId()) || !valid(value.getAttributeId()) || !valid(value.getDeviceId()) || !valid(value.getEventId()) || (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0))) return Mono.error(new RequestException("tenantId, attributeId, deviceId and eventId are required")); return Mono.defer(() -> Mono.zip(attributeService.getById(value.getTenantId(), value.getAttributeId()), deviceService.getById(value.getTenantId(), value.getDeviceId()), eventService.getById(value.getTenantId(), value.getEventId()))).switchIfEmpty(Mono.error(new NotFoundException("Resource does not exist"))).flatMap(tuple -> { EventAttributeBO attribute = tuple.getT1(); DeviceBO device = tuple.getT2(); EventBO event = tuple.getT3(); return Objects.equals(attribute.getDriverId(), device.getDriverId()) && Objects.equals(device.getProfileId(), event.getProfileId()) ? Mono.empty() : Mono.error(new NotFoundException("Resource does not exist")); }); }
    private EventAttributeConfigBO normalize(EventAttributeConfigBO value, boolean update) { if (value.getEnableFlag() == null) value.setEnableFlag(io.github.pnoker.common.enums.EnableFlagEnum.ENABLE); if (!update && value.getVersion() == null) value.setVersion(0); return value; }
    private void publish(Long tenantId, Long deviceId) { if (tenantId != null && deviceId != null) metadataEventPublisher.publishEvent(new MetadataEvent(this, tenantId, deviceId, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE)); }
    private boolean valid(Long value) { return value != null && value > 0; }
}
