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

import io.github.pnoker.common.data.biz.DriverAlarmService;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.enums.AlarmSourceFlagEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Business service implementation for driver alarm event persistence.
 *
 * <p>See {@link DeviceAlarmServiceImpl} for the rationale behind the tenant-id
 * backfill — the same silent-drop hazard exists on the driver path.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverAlarmServiceImpl implements DriverAlarmService {

    private final EntityAlarmManager entityAlarmManager;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    private final DriverFacade driverFacade;

    @Override
    public void alarm(DriverAlarmDTO entityDTO) {
        if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDriverId())) {
            log.warn("Drop driver alarm without driverId: {}", entityDTO);
            return;
        }

        Long tenantId = entityDTO.getTenantId();
        if (Objects.isNull(tenantId) || tenantId <= 0) {
            FacadeDriverBO driver = driverFacade.getById(entityDTO.getDriverId());
            if (Objects.isNull(driver)) {
                log.warn("Drop driver alarm because driver[{}] is not found in metadata; tenant context unavailable",
                        entityDTO.getDriverId());
                return;
            }
            tenantId = driver.getTenantId();
        }
        if (Objects.isNull(tenantId) || tenantId <= 0) {
            log.warn("Drop driver alarm because tenantId could not be resolved, driverId={}", entityDTO.getDriverId());
            return;
        }
        entityDTO.setTenantId(tenantId);

        String msg = Objects.nonNull(entityDTO.getMessage()) ? entityDTO.getMessage() : "driver-alarm";
        EntityAlarmDO entity = new EntityAlarmDO();
        entity.setAlarmTargetTypeFlag(AlarmTargetTypeFlagEnum.DRIVER.getIndex());
        entity.setEntityId(entityDTO.getDriverId());
        entity.setDriverId(entityDTO.getDriverId());
        entity.setDeviceId(0L);
        entity.setPointId(0L);
        entity.setRuleId(0L);
        entity.setAlarmTypeFlag(AlarmTypeFlagEnum.REPORT.getIndex());
        entity.setAlarmSourceFlag(AlarmSourceFlagEnum.DRIVER_REPORT.getIndex());
        // Driver-reported alarms default to P2; rule-driven severity is set when
        // the rule pipeline writes a follow-up entity_alarm row.
        entity.setAlarmLevelFlag(AlarmMessageLevelFlagEnum.P2.getIndex());
        entity.setAlarmExt(JsonExt.builder().type("driver-alarm").content(msg).version(1).build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(tenantId);
        entityAlarmManager.save(entity);

        entityDTO.setAlarmId(entity.getId());
        alarmRuleTriggerService.processDriverAlarm(entityDTO);
    }

}
