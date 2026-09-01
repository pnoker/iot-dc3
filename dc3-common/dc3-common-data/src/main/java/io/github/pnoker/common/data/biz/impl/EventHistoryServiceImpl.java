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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.data.biz.EventHistoryService;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.entity.bo.EventReportBO;
import io.github.pnoker.common.data.entity.builder.EventHistoryBuilder;
import io.github.pnoker.common.data.entity.model.EventHistoryDO;
import io.github.pnoker.common.data.entity.vo.EventHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.EventHistoryVO;
import io.github.pnoker.common.entity.dto.EventReportDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventHistoryAcknowledgeFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.query.FacadeEventOffsetQuery;
import io.github.pnoker.common.data.repository.ReactiveEventHistoryStore;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.List;

/**
 * Business service implementation for event report operations.
 *
 * @author pnoker
 * @since 2026.5.23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventHistoryServiceImpl implements EventHistoryService {

    private final DeviceFacade deviceFacade;

    private final EventFacade eventFacade;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    private final ReactiveEventHistoryStore eventHistoryStore;

    private final EventHistoryBuilder eventHistoryBuilder;

    @Override
    public Mono<String> report(Long tenantId, EventReportBO entityBO) {
        return validateEventScopeReactive(tenantId, entityBO.getDeviceId(), entityBO.getEventId(), entityBO.getEventCode())
                .flatMap(event -> {
                    String recordId = UUID.randomUUID().toString();
                    LocalDateTime now = LocalDateTime.now(java.time.ZoneOffset.UTC);
                    EventHistoryDO recordDO = new EventHistoryDO();
                    recordDO.setRecordId(recordId);
                    recordDO.setTenantId(tenantId);
                    recordDO.setDeviceId(entityBO.getDeviceId());
                    recordDO.setEventId(event.getId());
                    recordDO.setEventCode(event.getEventCode());
                    recordDO.setEventTypeFlag(event.getEventTypeFlag().getIndex());
                    recordDO.setEventLevelFlag(event.getEventLevelFlag().getIndex());
                    recordDO.setParamValues(entityBO.getParamValues() == null ? null : JsonUtil.toJsonString(entityBO.getParamValues()));
                    recordDO.setMessage(entityBO.getMessage());
                    recordDO.setOccurTime(now);
                    recordDO.setReceiveTime(now);
                    recordDO.setAcknowledgeFlag(EventHistoryAcknowledgeFlagEnum.NO.getIndex());
                    recordDO.setSchemaVersion((short) 1);
                    return eventHistoryStore.insert(recordDO).thenReturn(recordId);
                });
    }

    @Override
    public Mono<String> report(EventReportDTO entityDTO) {
        return validateEventScopeReactive(entityDTO.tenantId(), entityDTO.deviceId(), entityDTO.eventId(), entityDTO.eventCode())
                .flatMap(event -> {
                    LocalDateTime now = LocalDateTime.now(java.time.ZoneOffset.UTC);
                    EventHistoryDO recordDO = new EventHistoryDO();
                    recordDO.setRecordId(entityDTO.recordId());
                    recordDO.setTenantId(entityDTO.tenantId());
                    recordDO.setDeviceId(entityDTO.deviceId());
                    recordDO.setEventId(event.getId());
                    recordDO.setEventCode(event.getEventCode());
                    recordDO.setEventTypeFlag(entityDTO.eventTypeFlag());
                    recordDO.setEventLevelFlag(entityDTO.eventLevelFlag());
                    recordDO.setParamValues(entityDTO.paramValues() == null ? null : JsonUtil.toJsonString(entityDTO.paramValues()));
                    recordDO.setConfigSnapshot(entityDTO.configSnapshot());
                    recordDO.setMessage(entityDTO.message());
                    recordDO.setOccurTime(entityDTO.occurTime() == null ? now
                            : LocalDateTime.ofInstant(entityDTO.occurTime(), java.time.ZoneOffset.UTC));
                    recordDO.setReceiveTime(now);
                    recordDO.setAcknowledgeFlag(EventHistoryAcknowledgeFlagEnum.NO.getIndex());
                    recordDO.setSchemaVersion((short) entityDTO.schemaVersion());
                    return eventHistoryStore.insert(recordDO)
                            .then(alarmRuleTriggerService.processEventReport(entityDTO))
                            .thenReturn(entityDTO.recordId());
                });
    }

    @Override
    public Mono<EventHistoryVO> getByRecordId(Long tenantId, String recordId) {
        return eventHistoryStore.findByRecordId(tenantId, recordId).map(eventHistoryBuilder::buildVOByDO);
    }

    @Override
    public Mono<OffsetPage<EventHistoryVO>> list(Long tenantId, EventHistoryQueryVO queryVO) {
        EventHistoryQueryVO query = queryVO == null ? new EventHistoryQueryVO() : queryVO;
        Long deviceId = parseId(query.getDeviceId());
        Long eventId = parseId(query.getEventId());
        Byte eventType = query.getEventTypeFlag() == null ? null : query.getEventTypeFlag().getIndex();
        return eventHistoryStore.list(tenantId, deviceId, eventId, query.getEventCode(), eventType,
                        query.getOffset(), query.getLimit(), query.getSort())
                .map(page -> OffsetPage.of(page.items().stream().map(eventHistoryBuilder::buildVOByDO).toList(),
                        page.offset(), page.limit(), page.total()));
    }

    private Mono<FacadeEventBO> validateEventScopeReactive(Long tenantId, Long deviceId, Long eventId, String eventCode) {
        return deviceFacade.getByIdReactive(tenantId, deviceId)
                .switchIfEmpty(Mono.error(new NotFoundException("Device does not exist")))
                .flatMap(device -> {
                    if (EnableFlagEnum.DISABLE.equals(device.getEnableFlag())) {
                        return Mono.error(new ServiceException("Device is disabled"));
                    }
                    return resolveEventReactive(tenantId, device, eventId, eventCode)
                            .switchIfEmpty(Mono.error(new NotFoundException("Event does not exist")))
                            .flatMap(event -> {
                                if (EnableFlagEnum.DISABLE.equals(event.getEnableFlag())) {
                                    return Mono.error(new ServiceException("Event is disabled"));
                                }
                                if (device.getProfileId() == null || !Objects.equals(device.getProfileId(), event.getProfileId())) {
                                    return Mono.error(new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH));
                                }
                                return Mono.just(event);
                            });
                });
    }

    /**
     * Resolve an event by id when present, otherwise by code within the device's profile.
     * Requires at least one of event id or code.
     *
     * @param tenantId  tenant scope
     * @param device    the device whose profile scopes the lookup
     * @param eventId   the event id, used when present
     * @param eventCode the event code, used as fallback
     * @return the resolved event, or {@code null} when none matches
     */
    private Mono<FacadeEventBO> resolveEventReactive(Long tenantId, FacadeDeviceBO device, Long eventId, String eventCode) {
        if (eventId != null) {
            return eventFacade.getById(tenantId, eventId);
        }
        if (eventCode == null || eventCode.isBlank()) {
            return Mono.error(new ServiceException("Event id or code is required"));
        }
        return eventFacade.list(new FacadeEventOffsetQuery(tenantId, null, eventCode, null, null,
                device.getProfileId(), null, null, null, 0L, 1, List.of())).flatMapMany(page -> reactor.core.publisher.Flux.fromIterable(page.items())).next();
    }

    private Long parseId(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.valueOf(value); } catch (NumberFormatException exception) { throw new IllegalArgumentException("invalid numeric id: " + value, exception); }
    }

}
