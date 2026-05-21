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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.dto.DeviceAlarmDTO;
import io.github.pnoker.common.entity.dto.DriverAlarmDTO;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.utils.LocalDateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


/**
 * Alarm rule trigger service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmRuleTriggerServiceImpl implements AlarmRuleTriggerService {

    private final AlarmRulePipelineService alarmRulePipelineService;

    private final WindowSampleBuffer windowSampleBuffer;

    @Override
    public void processPointValue(PointValueBO pointValue) {
        if (Objects.isNull(pointValue) || !isValidId(pointValue.getTenantId()) || !isValidId(pointValue.getPointId())) {
            return;
        }

        // Append to the in-memory window buffer *before* dispatching to the
        // pipeline so the rule engine, when it asks for a window snapshot,
        // already sees the current sample. Keeps "evaluation includes the
        // triggering fact" intuition intact.
        LocalDateTime ts = factTime(pointValue.getCreateTime());
        windowSampleBuffer.append(
                WindowSampleKey.of(pointValue.getTenantId(), AlarmTargetTypeFlagEnum.POINT, pointValue.getPointId()),
                new WindowSample(pointValue.getNumValue(), pointValue.getCalValue(), ts));

        process(new RuleFact(
                pointValue.getTenantId(),
                AlarmTargetTypeFlagEnum.POINT,
                pointValue.getPointId(),
                null,
                ts,
                RuleFactValues.point(pointValue)));
    }

    @Override
    public void processPointValues(List<PointValueBO> pointValues) {
        if (CollectionUtils.isEmpty(pointValues)) {
            return;
        }
        // The pipeline still evaluates each fact through RuleEngine per-call; the
        // bulk entrypoint exists so callers (notably PointValueServiceImpl) can
        // hand off the whole batch in one go and the engine's cached lookups
        // (RuleRegistry) are amortized across the group. Per-fact pipeline
        // execution is intentional — alarm semantics (firing, recovery, dedup)
        // are still defined sample-by-sample.
        for (PointValueBO pointValue : pointValues) {
            processPointValue(pointValue);
        }
    }

    @Override
    public void processDeviceAlarm(DeviceAlarmDTO alarm) {
        if (Objects.isNull(alarm) || !isValidId(alarm.getTenantId()) || !isValidId(alarm.getDeviceId())) {
            return;
        }

        process(new RuleFact(
                alarm.getTenantId(),
                AlarmTargetTypeFlagEnum.DEVICE,
                alarm.getDeviceId(),
                alarm.getAlarmId(),
                factTime(alarm.getCreateTime()),
                RuleFactValues.deviceAlarm(alarm)));
    }

    @Override
    public void processDriverAlarm(DriverAlarmDTO alarm) {
        if (Objects.isNull(alarm) || !isValidId(alarm.getTenantId()) || !isValidId(alarm.getDriverId())) {
            return;
        }

        process(new RuleFact(
                alarm.getTenantId(),
                AlarmTargetTypeFlagEnum.DRIVER,
                alarm.getDriverId(),
                alarm.getAlarmId(),
                factTime(alarm.getCreateTime()),
                RuleFactValues.driverAlarm(alarm)));
    }

    private void process(RuleFact fact) {
        try {
            alarmRulePipelineService.process(fact);
        } catch (Exception e) {
            log.error("Alarm rule pipeline failed, tenantId={}, targetType={}, entityId={}",
                    fact.getTenantId(), fact.getAlarmTargetTypeFlag(), fact.getEntityId(), e);
        }
    }

    private LocalDateTime factTime(LocalDateTime time) {
        return Objects.nonNull(time) ? time : LocalDateTimeUtil.now();
    }

    private boolean isValidId(Long id) {
        return Objects.nonNull(id) && id > 0;
    }

}
