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
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Business service implementation for device alarm event persistence.
 *
 * <p>Backfills {@code tenantId} via {@link DeviceFacade#getById(Long)} when the
 * incoming DTO did not carry it — without this, alarms with a missing tenant
 * would be persisted with {@code tenant_id = 0} and then silently dropped by
 * the rule trigger (which requires a valid tenant id), losing the user-visible
 * notification entirely. If neither the DTO nor the device record can supply a
 * tenant id, the alarm is dropped at the entrypoint rather than written to the
 * database.
 *
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

    private final DeviceFacade deviceFacade;

    @Override
    public void alarm(DeviceAlarmDTO entityDTO) {
        if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDeviceId())) {
            log.warn("Drop device alarm without deviceId: {}", entityDTO);
            return;
        }

        FacadeDeviceBO device = null;
        Long tenantId = entityDTO.getTenantId();
        Long driverId = entityDTO.getDriverId();
        if (Objects.isNull(tenantId) || tenantId <= 0 || Objects.isNull(driverId) || driverId <= 0) {
            device = deviceFacade.getById(entityDTO.getDeviceId());
            if (Objects.isNull(device)) {
                log.warn("Drop device alarm because device[{}] is not found in metadata; tenant context unavailable",
                        entityDTO.getDeviceId());
                return;
            }
            if (Objects.isNull(tenantId) || tenantId <= 0) {
                tenantId = device.getTenantId();
            }
            if (Objects.isNull(driverId) || driverId <= 0) {
                driverId = device.getDriverId();
            }
        }
        if (Objects.isNull(tenantId) || tenantId <= 0) {
            log.warn("Drop device alarm because tenantId could not be resolved, deviceId={}", entityDTO.getDeviceId());
            return;
        }
        entityDTO.setTenantId(tenantId);
        entityDTO.setDriverId(Objects.requireNonNullElse(driverId, 0L));

        String msg = Objects.nonNull(entityDTO.getMessage()) ? entityDTO.getMessage() : "device-alarm";
        EntityAlarmDO entity = new EntityAlarmDO();
        entity.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.DEVICE.getIndex());
        entity.setEntityId(entityDTO.getDeviceId());
        entity.setDriverId(Objects.requireNonNullElse(driverId, 0L));
        entity.setDeviceId(entityDTO.getDeviceId());
        entity.setPointId(0L);
        entity.setRuleId(0L);
        entity.setAlarmTypeFlag(AlarmTypeFlagEnum.REPORT.getIndex());
        entity.setAlarmSourceFlag(AlarmSourceFlagEnum.DEVICE_REPORT.getIndex());
        entity.setAlarmExt(JsonExt.builder().type("device-alarm").content(msg).version(1).build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(tenantId);
        entityAlarmManager.save(entity);

        entityDTO.setAlarmId(entity.getId());
        alarmRuleTriggerService.processDeviceAlarm(entityDTO);
    }

}
