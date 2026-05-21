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

import io.github.pnoker.common.data.biz.DeviceAlarmService;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmSourceFlagEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Business service implementation for device alarm event persistence.
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAlarmServiceImpl implements DeviceAlarmService {

    private final EntityAlarmManager entityAlarmManager;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    @Override
    public void alarm(DeviceAlarmDTO entityDTO) {
        if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDeviceId())) {
            log.warn("Drop device alarm without deviceId: {}", entityDTO);
            return;
        }

        String msg = Objects.nonNull(entityDTO.getMessage()) ? entityDTO.getMessage() : "device-alarm";
        EntityAlarmDO entity = new EntityAlarmDO();
        entity.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.DEVICE.getIndex());
        entity.setEntityId(entityDTO.getDeviceId());
        entity.setDriverId(Objects.requireNonNullElse(entityDTO.getDriverId(), 0L));
        entity.setDeviceId(entityDTO.getDeviceId());
        entity.setPointId(0L);
        entity.setRuleId(0L);
        entity.setAlarmTypeFlag(AlarmTypeFlagEnum.REPORT.getIndex());
        entity.setAlarmSourceFlag(AlarmSourceFlagEnum.DEVICE_REPORT.getIndex());
        entity.setAlarmExt(JsonExt.builder().type("device-alarm").content(msg).version(1).build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(Objects.nonNull(entityDTO.getTenantId()) ? entityDTO.getTenantId() : 0L);
        entityAlarmManager.save(entity);

        entityDTO.setAlarmId(entity.getId());
        alarmRuleTriggerService.processDeviceAlarm(entityDTO);
    }

}
