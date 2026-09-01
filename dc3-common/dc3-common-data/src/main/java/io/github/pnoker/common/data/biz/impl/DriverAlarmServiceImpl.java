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
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.data.repository.ReactiveEntityAlarmStore;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmMessageLevelEnum;
import io.github.pnoker.common.enums.AlarmSourceTypeEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.enums.AlarmTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Business service implementation for driver alarm event persistence.
 *
 * <p>See {@link DeviceAlarmServiceImpl} for the rationale behind the tenant-id
 * backfill — the same silent-drop hazard exists on the driver path.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverAlarmServiceImpl implements DriverAlarmService {

    private final ReactiveEntityAlarmStore entityAlarmStore;

    private final AlarmRuleTriggerService alarmRuleTriggerService;

    @Override
    public Mono<Void> alarm(DriverAlarmDTO entityDTO) {
        if (Objects.isNull(entityDTO) || Objects.isNull(entityDTO.getDriverId())) {
            log.warn("Driver alarm dropped, reason=missingDriverId, tenantId={}",
                    Objects.nonNull(entityDTO) ? entityDTO.getTenantId() : null);
            return Mono.empty();
        }

        Long tenantId = entityDTO.getTenantId();
        if (Objects.isNull(tenantId) || tenantId <= 0) {
            // See DeviceAlarmServiceImpl: tenant must come from the upstream source; the
            // fail-closed interceptor forbids reverse-resolving it from the driver.
            log.warn("Driver alarm dropped, reason=missingTenantId, driverId={}", entityDTO.getDriverId());
            return Mono.empty();
        }
        entityDTO.setTenantId(tenantId);

        String msg = Objects.nonNull(entityDTO.getMessage()) ? entityDTO.getMessage() : "driver-alarm";
        EntityAlarmDO entity = new EntityAlarmDO();
        entity.setAlarmTargetTypeFlag(AlarmTargetTypeEnum.DRIVER.getIndex());
        entity.setEntityId(entityDTO.getDriverId());
        entity.setDriverId(entityDTO.getDriverId());
        entity.setDeviceId(0L);
        entity.setPointId(0L);
        entity.setRuleId(0L);
        entity.setAlarmTypeFlag(AlarmTypeEnum.REPORT.getIndex());
        entity.setAlarmSourceFlag(AlarmSourceTypeEnum.DRIVER_REPORT.getIndex());
        // Driver-reported alarms default to P2; rule-driven severity is set when
        // the rule pipeline writes a follow-up entity_alarm row.
        entity.setAlarmLevelFlag(AlarmMessageLevelEnum.P2.getIndex());
        entity.setAlarmExt(JsonExt.builder().type("driver-alarm").content(msg).version(1).build());
        entity.setExpiredTime(0L);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(tenantId);
        return entityAlarmStore.insert(entity)
                .flatMap(saved -> {
                    entityDTO.setAlarmId(saved.getId());
                    return alarmRuleTriggerService.processDriverAlarm(entityDTO);
                }).then();
    }

}
