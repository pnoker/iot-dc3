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
package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.ConflictException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.repository.EventFilter;
import io.github.pnoker.common.manager.repository.ReactiveEventParamStore;
import io.github.pnoker.common.manager.repository.ReactiveEventStore;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;
import io.github.pnoker.common.manager.service.ReactiveEventService;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Default event service implementation. */
@Service
@RequiredArgsConstructor
public class ReactiveEventServiceImpl implements ReactiveEventService {
    private final ReactiveEventStore eventStore;
    private final ReactiveProfileStore profileStore;
    private final io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher metadataEventPublisher;
    private final ReactiveEventParamStore eventParamStore;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<EventBO> add(EventBO value) {
        return Mono.defer(() -> {
            validate(value, false);
            value.setEventCode(
                    value.getEventCode() == null || value.getEventCode().isBlank()
                            ? CodeUtil.getCode()
                            : value.getEventCode().trim());
            return ensureProfile(value)
                    .then(eventStore.existsByNameOrCode(
                            value.getTenantId(),
                            value.getProfileId(),
                            value.getEventName(),
                            value.getEventCode(),
                            null))
                    .flatMap(duplicate -> duplicate
                            ? Mono.<EventBO>error(new DuplicateException("Event has been duplicated"))
                            : eventStore.insert(normalize(value, false)))
                    .onErrorMap(
                            DataIntegrityViolationException.class,
                            error -> new DuplicateException("Event has been duplicated"))
                    .doOnSuccess(saved -> publish(saved, MetadataOperateTypeEnum.ADD));
        });
    }

    @Override
    public Mono<EventBO> update(EventBO value) {
        return Mono.defer(() -> {
            validate(value, true);
            return eventStore
                    .get(value.getTenantId(), value.getId())
                    .switchIfEmpty(Mono.error(new NotFoundException("Event does not exist")))
                    .flatMap(current -> ensureProfile(value)
                            .then(eventStore.existsByNameOrCode(
                                    value.getTenantId(),
                                    value.getProfileId(),
                                    value.getEventName(),
                                    current.getEventCode(),
                                    value.getId()))
                            .flatMap(duplicate -> duplicate
                                    ? Mono.<EventBO>error(new DuplicateException("Event has been duplicated"))
                                    : eventStore.update(normalize(value, true), value.getVersion())))
                    .switchIfEmpty(Mono.error(new ConflictException("Event version conflict")))
                    .doOnSuccess(saved -> publish(saved, MetadataOperateTypeEnum.UPDATE));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName) {
        if (tenantId == null || id == null)
            return Mono.error(new RequestException("Tenant ID and event ID are required"));
        Mono<Boolean> deletion = eventStore
                .get(tenantId, id)
                .switchIfEmpty(Mono.error(new NotFoundException("Event does not exist")))
                .flatMap(event -> eventParamStore
                        .deleteByEventId(tenantId, id, operatorId, operatorName)
                        .then(eventStore.delete(tenantId, id, expectedVersion, operatorId, operatorName)))
                .filter(Boolean.TRUE::equals)
                .switchIfEmpty(Mono.error(new ConflictException("Event version conflict")));
        return transactionalOperator
                .transactional(deletion)
                .doOnSuccess(ignored -> metadataEventPublisher.publishEvent(
                        new MetadataEvent(this, tenantId, id, MetadataTypeEnum.EVENT, MetadataOperateTypeEnum.DELETE)));
    }

    @Override
    public Mono<EventBO> getById(Long tenantId, Long id) {
        if (tenantId == null || id == null)
            return Mono.error(new RequestException("Tenant ID and event ID are required"));
        return eventStore.get(tenantId, id).switchIfEmpty(Mono.error(new NotFoundException("Event does not exist")));
    }

    @Override
    public Flux<EventBO> listByIds(Long tenantId, List<Long> ids) {
        return eventStore.listByIds(tenantId, ids);
    }

    @Override
    public Flux<EventBO> listByProfileId(Long tenantId, Long profileId) {
        return eventStore.listByProfileId(tenantId, profileId);
    }

    @Override
    public Flux<EventBO> listByDeviceId(Long tenantId, Long deviceId) {
        return eventStore.listByDeviceId(tenantId, deviceId);
    }

    @Override
    public Mono<OffsetPage<EventBO>> list(EventFilter filter) {
        return eventStore.list(filter);
    }

    private Mono<Void> ensureProfile(EventBO value) {
        return profileStore
                .get(value.getTenantId(), value.getProfileId())
                .switchIfEmpty(Mono.error(new NotFoundException("Profile does not exist")))
                .then();
    }

    private void validate(EventBO value, boolean update) {
        if (value == null
                || value.getTenantId() == null
                || value.getTenantId() <= 0
                || value.getProfileId() == null
                || value.getProfileId() <= 0
                || value.getEventName() == null
                || value.getEventName().isBlank())
            throw new RequestException("Tenant ID, profile ID and event name are required");
        if (update && (value.getId() == null || value.getVersion() == null || value.getVersion() < 0))
            throw new RequestException("Event ID and version are required for update");
        value.setEventName(value.getEventName().trim());
    }

    private EventBO normalize(EventBO source, boolean update) {
        source.setEventTypeFlag(source.getEventTypeFlag() == null ? EventTypeFlagEnum.INFO : source.getEventTypeFlag());
        source.setEventLevelFlag(source.getEventLevelFlag() == null ? EventLevelEnum.LOW : source.getEventLevelFlag());
        source.setEnableFlag(source.getEnableFlag() == null ? EnableFlagEnum.ENABLE : source.getEnableFlag());
        source.setRemark(source.getRemark() == null ? "" : source.getRemark());
        if (!update && source.getVersion() == null) source.setVersion(0);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!update && source.getCreateTime() == null) source.setCreateTime(now);
        source.setOperateTime(now);
        return source;
    }

    private void publish(EventBO value, MetadataOperateTypeEnum operation) {
        if (value != null)
            metadataEventPublisher.publishEvent(
                    new MetadataEvent(this, value.getTenantId(), value.getId(), MetadataTypeEnum.EVENT, operation));
    }
}
