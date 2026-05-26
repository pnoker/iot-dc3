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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.data.biz.EventHistoryService;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EventHistoryManager;
import io.github.pnoker.common.data.entity.model.EventHistoryDO;
import io.github.pnoker.common.data.entity.vo.EventHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.EventReportVO;
import io.github.pnoker.common.entity.common.Pages;
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
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeEventQuery;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Business service implementation for event report operations.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventHistoryServiceImpl implements EventHistoryService {

    private final DeviceFacade deviceFacade;

    private final EventFacade eventFacade;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    private final EventHistoryManager eventHistoryManager;

    @Override
    public String report(Long tenantId, EventReportVO entityVO) {
        FacadeEventBO event = validateEventScope(tenantId, entityVO.getDeviceId(), entityVO.getEventId(),
                entityVO.getEventCode());

        String recordId = UUID.randomUUID().toString();
        LocalDateTime nowLocal = LocalDateTime.now();

        EventHistoryDO recordDO = new EventHistoryDO();
        recordDO.setRecordId(recordId);
        recordDO.setTenantId(tenantId);
        recordDO.setDeviceId(entityVO.getDeviceId());
        recordDO.setEventId(event.getId());
        recordDO.setEventCode(event.getEventCode());
        recordDO.setEventTypeFlag(event.getEventTypeFlag().getIndex());
        recordDO.setEventLevelFlag(event.getEventLevelFlag().getIndex());
        recordDO.setParamValues(Objects.isNull(entityVO.getParamValues()) ? null : JsonUtil.toJsonString(entityVO.getParamValues()));
        recordDO.setMessage(entityVO.getMessage());
        recordDO.setOccurTime(nowLocal);
        recordDO.setReceiveTime(nowLocal);
        recordDO.setAcknowledgeFlag(EventHistoryAcknowledgeFlagEnum.NO.getIndex());
        recordDO.setSchemaVersion((short) 1);
        eventHistoryManager.save(recordDO);

        return recordId;
    }

    @Override
    public String report(EventReportDTO entityDTO) {
        validateEventScope(entityDTO.tenantId(), entityDTO.deviceId(), entityDTO.eventId(), entityDTO.eventCode());

        LocalDateTime nowLocal = LocalDateTime.now();

        EventHistoryDO recordDO = new EventHistoryDO();
        recordDO.setRecordId(entityDTO.recordId());
        recordDO.setTenantId(entityDTO.tenantId());
        recordDO.setDeviceId(entityDTO.deviceId());
        recordDO.setEventId(entityDTO.eventId());
        recordDO.setEventCode(entityDTO.eventCode());
        recordDO.setEventTypeFlag(entityDTO.eventTypeFlag());
        recordDO.setEventLevelFlag(entityDTO.eventLevelFlag());
        recordDO.setParamValues(Objects.isNull(entityDTO.paramValues()) ? null : JsonUtil.toJsonString(entityDTO.paramValues()));
        recordDO.setConfigSnapshot(entityDTO.configSnapshot());
        recordDO.setMessage(entityDTO.message());
        recordDO.setOccurTime(Objects.nonNull(entityDTO.occurTime())
                ? LocalDateTime.ofInstant(entityDTO.occurTime(), ZoneId.systemDefault()) : nowLocal);
        recordDO.setReceiveTime(nowLocal);
        recordDO.setAcknowledgeFlag(EventHistoryAcknowledgeFlagEnum.NO.getIndex());
        recordDO.setSchemaVersion((short) entityDTO.schemaVersion());
        eventHistoryManager.save(recordDO);

        alarmRuleTriggerService.processEventReport(entityDTO);

        return entityDTO.recordId();
    }

    @Override
    public EventHistoryDO getByRecordId(Long tenantId, String recordId) {
        return eventHistoryManager.lambdaQuery()
                .eq(Objects.nonNull(tenantId), EventHistoryDO::getTenantId, tenantId)
                .eq(EventHistoryDO::getRecordId, recordId)
                .one();
    }

    @Override
    public Page<EventHistoryDO> list(Long tenantId, EventHistoryQueryVO queryVO) {
        LambdaQueryWrapper<EventHistoryDO> wrapper = new LambdaQueryWrapper<EventHistoryDO>()
                .eq(EventHistoryDO::getTenantId, tenantId)
                .eq(Objects.nonNull(queryVO.getDeviceId()), EventHistoryDO::getDeviceId, queryVO.getDeviceId())
                .eq(Objects.nonNull(queryVO.getEventId()), EventHistoryDO::getEventId, queryVO.getEventId())
                .eq(Objects.nonNull(queryVO.getEventTypeFlag()), EventHistoryDO::getEventTypeFlag, queryVO.getEventTypeFlag())
                .orderByDesc(EventHistoryDO::getOccurTime);
        return eventHistoryManager.page(queryVO.toPage(), wrapper);
    }

    private FacadeEventBO validateEventScope(Long tenantId, Long deviceId, Long eventId, String eventCode) {
        FacadeDeviceBO device = deviceFacade.getById(tenantId, deviceId);
        if (Objects.isNull(device)) {
            throw new NotFoundException("Device does not exist");
        }
        if (EnableFlagEnum.DISABLE.equals(device.getEnableFlag())) {
            throw new ServiceException("Device is disabled");
        }

        FacadeEventBO event = resolveEvent(tenantId, device, eventId, eventCode);
        if (Objects.isNull(event)) {
            throw new NotFoundException("Event does not exist");
        }
        if (EnableFlagEnum.DISABLE.equals(event.getEnableFlag())) {
            throw new ServiceException("Event is disabled");
        }
        if (Objects.isNull(device.getProfileId()) || !Objects.equals(device.getProfileId(), event.getProfileId())) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        return event;
    }

    private FacadeEventBO resolveEvent(Long tenantId, FacadeDeviceBO device, Long eventId, String eventCode) {
        if (Objects.nonNull(eventId)) {
            return eventFacade.getById(tenantId, eventId);
        }
        if (StringUtils.isBlank(eventCode)) {
            throw new ServiceException("Event id or code is required");
        }

        Pages page = new Pages();
        page.setSize(1);
        FacadePage<FacadeEventBO> eventPage = eventFacade.listByPage(FacadeEventQuery.builder()
                .page(page)
                .tenantId(tenantId)
                .profileId(device.getProfileId())
                .eventCode(eventCode)
                .build());
        if (Objects.isNull(eventPage) || Objects.isNull(eventPage.getRecords()) || eventPage.getRecords().isEmpty()) {
            return null;
        }
        return eventPage.getRecords().get(0);
    }

}
