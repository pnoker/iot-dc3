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
import io.github.pnoker.common.enums.AlarmMessageLevelEnum;
import io.github.pnoker.common.enums.AlarmSourceTypeEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.AlarmTypeEnum;
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

        Long tenantId = entityDTO.getTenantId();
        if (Objects.isNull(tenantId) || tenantId <= 0) {
            // Tenant id must be supplied by the upstream alarm source (driver report, timeout
            // check, state scan). The fail-closed tenant-line interceptor rejects unscoped
            // queries, so the tenant can no longer be reverse-resolved from the device — drop
            // instead of silently persisting with tenant_id=0.
            log.warn("Drop device alarm because tenantId is missing, deviceId={}", entityDTO.getDeviceId());
            return;
        }
        Long driverId = entityDTO.getDriverId();
        FacadeDeviceBO device = null;
        if (Objects.isNull(driverId) || driverId <= 0) {
            device = deviceFacade.getById(tenantId, entityDTO.getDeviceId());
            if (Objects.isNull(device)) {
                log.warn("Drop device alarm because device[{}] is not found in metadata", entityDTO.getDeviceId());
                return;
            }
            driverId = device.getDriverId();
        }
        entityDTO.setTenantId(tenantId);
        entityDTO.setDriverId(Objects.requireNonNullElse(driverId, 0L));

        String msg = Objects.nonNull(entityDTO.getMessage()) ? entityDTO.getMessage() : "device-alarm";
        EntityAlarmDO entity = new EntityAlarmDO();
        entity.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.DEVICE.getIndex());
        entity.setEntityId(entityDTO.getDeviceId());
        entity.setDriverId(Objects.requireNonNullElse(driverId, 0L));
        entity.setDeviceId(entityDTO.getDeviceId());
        entity.setPointId(0L);
        entity.setRuleId(0L);
        entity.setAlarmTypeFlag(AlarmTypeEnum.REPORT.getIndex());
        entity.setAlarmSourceFlag(AlarmSourceTypeEnum.DEVICE_REPORT.getIndex());
        // Device-reported alarms default to P2; rule-driven severity is set when
        // the rule pipeline writes a follow-up entity_alarm row.
        entity.setAlarmLevelFlag(AlarmMessageLevelEnum.P2.getIndex());
        entity.setAlarmExt(JsonExt.builder().type("device-alarm").content(msg).version(1).build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(tenantId);
        entityAlarmManager.save(entity);

        entityDTO.setAlarmId(entity.getId());
        alarmRuleTriggerService.processDeviceAlarm(entityDTO);
    }

}
