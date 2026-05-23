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
import io.github.pnoker.common.data.biz.EventReportService;
import io.github.pnoker.common.data.dal.EventRecordManager;
import io.github.pnoker.common.data.entity.model.EventRecordDO;
import io.github.pnoker.common.data.entity.vo.EventRecordQueryVO;
import io.github.pnoker.common.data.entity.vo.EventReportVO;
import io.github.pnoker.common.entity.dto.EventReportDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventRecordAcknowledgeFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class EventReportServiceImpl implements EventReportService {

    private final DeviceFacade deviceFacade;

    private final EventFacade eventFacade;

    private final EventRecordManager eventRecordManager;

    @Override
    public String report(Long tenantId, EventReportVO entityVO) {
        validateEventScope(tenantId, entityVO.getDeviceId(), entityVO.getEventId());

        FacadeEventBO event = eventFacade.getById(tenantId, entityVO.getEventId());

        String recordId = UUID.randomUUID().toString();
        LocalDateTime nowLocal = LocalDateTime.now();

        EventRecordDO recordDO = new EventRecordDO();
        recordDO.setRecordId(recordId);
        recordDO.setTenantId(tenantId);
        recordDO.setDeviceId(entityVO.getDeviceId());
        recordDO.setEventId(entityVO.getEventId());
        recordDO.setEventCode(event.getEventCode());
        recordDO.setEventTypeFlag(event.getEventTypeFlag().getIndex());
        recordDO.setEventLevelFlag(event.getEventLevelFlag().getIndex());
        recordDO.setParamValues(Objects.isNull(entityVO.getParamValues()) ? null : entityVO.getParamValues().toString());
        recordDO.setMessage(entityVO.getMessage());
        recordDO.setOccurTime(nowLocal);
        recordDO.setReceiveTime(nowLocal);
        recordDO.setAcknowledgeFlag(EventRecordAcknowledgeFlagEnum.NO.getIndex());
        recordDO.setSchemaVersion((short) 1);
        eventRecordManager.save(recordDO);

        return recordId;
    }

    @Override
    public String report(EventReportDTO entityDTO) {
        validateEventScope(entityDTO.tenantId(), entityDTO.deviceId(), entityDTO.eventId());

        LocalDateTime nowLocal = LocalDateTime.now();

        EventRecordDO recordDO = new EventRecordDO();
        recordDO.setRecordId(entityDTO.recordId());
        recordDO.setTenantId(entityDTO.tenantId());
        recordDO.setDeviceId(entityDTO.deviceId());
        recordDO.setEventId(entityDTO.eventId());
        recordDO.setEventCode(entityDTO.eventCode());
        recordDO.setEventTypeFlag(entityDTO.eventTypeFlag());
        recordDO.setEventLevelFlag(entityDTO.eventLevelFlag());
        recordDO.setParamValues(Objects.isNull(entityDTO.paramValues()) ? null : JsonUtil.toJsonString(entityDTO.paramValues()));
        recordDO.setMessage(entityDTO.message());
        recordDO.setOccurTime(Objects.nonNull(entityDTO.occurTime())
                ? LocalDateTime.ofInstant(entityDTO.occurTime(), ZoneId.systemDefault()) : nowLocal);
        recordDO.setReceiveTime(nowLocal);
        recordDO.setAcknowledgeFlag(EventRecordAcknowledgeFlagEnum.NO.getIndex());
        recordDO.setSchemaVersion((short) entityDTO.schemaVersion());
        eventRecordManager.save(recordDO);

        return entityDTO.recordId();
    }

    @Override
    public EventRecordDO getByRecordId(String recordId) {
        return eventRecordManager.lambdaQuery()
                .eq(EventRecordDO::getRecordId, recordId)
                .one();
    }

    @Override
    public Page<EventRecordDO> list(Long tenantId, EventRecordQueryVO queryVO) {
        LambdaQueryWrapper<EventRecordDO> wrapper = new LambdaQueryWrapper<EventRecordDO>()
                .eq(EventRecordDO::getTenantId, tenantId)
                .eq(Objects.nonNull(queryVO.getDeviceId()), EventRecordDO::getDeviceId, queryVO.getDeviceId())
                .eq(Objects.nonNull(queryVO.getEventId()), EventRecordDO::getEventId, queryVO.getEventId())
                .eq(Objects.nonNull(queryVO.getEventTypeFlag()), EventRecordDO::getEventTypeFlag, queryVO.getEventTypeFlag())
                .orderByDesc(EventRecordDO::getOccurTime);
        return eventRecordManager.page(queryVO.toPage(), wrapper);
    }

    private void validateEventScope(Long tenantId, Long deviceId, Long eventId) {
        FacadeDeviceBO device = deviceFacade.getById(tenantId, deviceId);
        if (Objects.isNull(device)) {
            throw new NotFoundException("Device does not exist");
        }
        if (EnableFlagEnum.DISABLE.equals(device.getEnableFlag())) {
            throw new ServiceException("Device is disabled");
        }

        FacadeEventBO event = eventFacade.getById(tenantId, eventId);
        if (Objects.isNull(event)) {
            throw new NotFoundException("Event does not exist");
        }
        if (EnableFlagEnum.DISABLE.equals(event.getEnableFlag())) {
            throw new ServiceException("Event is disabled");
        }
        if (Objects.isNull(device.getProfileId()) || !Objects.equals(device.getProfileId(), event.getProfileId())) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
    }

}
