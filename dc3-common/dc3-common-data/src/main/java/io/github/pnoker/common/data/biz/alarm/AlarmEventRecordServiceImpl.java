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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.constant.service.AlarmConstant;
import io.github.pnoker.common.data.dal.DeviceEventManager;
import io.github.pnoker.common.data.dal.DriverEventManager;
import io.github.pnoker.common.data.dal.RuleStateManager;
import io.github.pnoker.common.data.entity.model.DeviceEventDO;
import io.github.pnoker.common.data.entity.model.DriverEventDO;
import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.RuleAlarmEventExt;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.DeviceEventTypeEnum;
import io.github.pnoker.common.enums.DriverEventTypeEnum;
import io.github.pnoker.common.enums.RuleStateFlagEnum;
import io.github.pnoker.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Alarm event record service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
public class AlarmEventRecordServiceImpl implements AlarmEventRecordService {

    private static final long DEFAULT_ID = 0L;

    @Resource
    private DeviceEventManager deviceEventManager;

    @Resource
    private DriverEventManager driverEventManager;

    @Resource
    private RuleStateManager ruleStateManager;

    @Override
    public void ensureEvent(RuleMatch match) {
        if (Objects.isNull(match) || Objects.isNull(match.getRule()) || Objects.isNull(match.getFact())) {
            return;
        }
        RuleFact fact = match.getFact();
        if (isValidId(fact.getEventId())) {
            return;
        }

        Long firingEventId = findFiringEventId(match);
        if (isValidId(firingEventId)) {
            fact.setEventId(firingEventId);
            return;
        }
        if (!StringUtils.equalsIgnoreCase(AlarmConstant.MATCH_TYPE_FIRING, match.getMatchType())) {
            return;
        }

        AlarmTargetTypeFlagEnum targetType = fact.getAlarmTargetTypeFlag();
        if (AlarmTargetTypeFlagEnum.POINT.equals(targetType) || AlarmTargetTypeFlagEnum.DEVICE.equals(targetType)) {
            Long eventId = persistDeviceEvent(match);
            if (isValidId(eventId)) {
                fact.setEventId(eventId);
            }
        } else if (AlarmTargetTypeFlagEnum.DRIVER.equals(targetType)) {
            Long eventId = persistDriverEvent(match);
            if (isValidId(eventId)) {
                fact.setEventId(eventId);
            }
        }
    }

    private Long persistDeviceEvent(RuleMatch match) {
        RuleFact fact = match.getFact();
        Long deviceId = AlarmTargetTypeFlagEnum.DEVICE.equals(fact.getAlarmTargetTypeFlag())
                ? fact.getEntityId() : longValue(fact.value("deviceId"));
        if (!isValidId(deviceId)) {
            log.warn("Skip rule device event because deviceId is missing, ruleId={}, targetType={}, entityId={}",
                    match.getRule().getId(), fact.getAlarmTargetTypeFlag(), fact.getEntityId());
            return null;
        }

        Long pointId = AlarmTargetTypeFlagEnum.POINT.equals(fact.getAlarmTargetTypeFlag())
                ? fact.getEntityId() : longValue(fact.value("pointId"));
        DeviceEventDO entity = new DeviceEventDO();
        entity.setDeviceId(deviceId);
        entity.setPointId(Objects.requireNonNullElse(pointId, DEFAULT_ID));
        entity.setEventTypeFlag(DeviceEventTypeEnum.ALARM.getIndex());
        entity.setEventExt(eventExt(match));
        entity.setExpiredTime(DEFAULT_ID);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(Objects.requireNonNullElse(fact.getTenantId(), DEFAULT_ID));
        if (!deviceEventManager.save(entity)) {
            log.warn("Failed to persist rule device event, ruleId={}, deviceId={}, pointId={}",
                    match.getRule().getId(), deviceId, pointId);
            return null;
        }
        return entity.getId();
    }

    private Long persistDriverEvent(RuleMatch match) {
        RuleFact fact = match.getFact();
        Long driverId = fact.getEntityId();
        if (!isValidId(driverId)) {
            log.warn("Skip rule driver event because driverId is missing, ruleId={}", match.getRule().getId());
            return null;
        }

        DriverEventDO entity = new DriverEventDO();
        entity.setDriverId(driverId);
        entity.setEventTypeFlag(DriverEventTypeEnum.ALARM.getIndex());
        entity.setEventExt(eventExt(match));
        entity.setExpiredTime(DEFAULT_ID);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(Objects.requireNonNullElse(fact.getTenantId(), DEFAULT_ID));
        if (!driverEventManager.save(entity)) {
            log.warn("Failed to persist rule driver event, ruleId={}, driverId={}", match.getRule().getId(), driverId);
            return null;
        }
        return entity.getId();
    }

    private JsonExt eventExt(RuleMatch match) {
        RuleAlarmEventExt ext = ruleAlarmEventExt(match);

        return JsonExt.builder()
                .type(ext.getType())
                .content(message(match))
                .remark(JsonUtil.toJsonString(ext.getContent()))
                .version(ext.getVersion())
                .build();
    }

    private String message(RuleMatch match) {
        String ruleName = StringUtils.defaultIfBlank(match.getRule().getRuleName(), match.getRule().getRuleCode());
        String eventType = StringUtils.defaultIfBlank(match.getEventType(), AlarmConstant.EXT_RULE_EVENT);
        return String.format("Rule %s fired: %s", ruleName, eventType);
    }

    private RuleAlarmEventExt ruleAlarmEventExt(RuleMatch match) {
        RuleAlarmEventExt ext = new RuleAlarmEventExt();
        ext.setType(AlarmConstant.EXT_RULE_EVENT);
        ext.setVersion(1);
        ext.setContent(new RuleAlarmEventExt.Content(
                match.getRule().getId(),
                match.getRule().getRuleCode(),
                match.getRule().getRuleName(),
                match.getFact().getAlarmTargetTypeFlag(),
                match.getFact().getEntityId(),
                match.getSeverity(),
                match.getEventType(),
                match.getMatchType(),
                Objects.requireNonNullElse(match.getFact().getValues(), Map.of())));
        return ext;
    }

    private Long findFiringEventId(RuleMatch match) {
        RuleFact fact = match.getFact();
        if (!isValidId(match.getRule().getId()) || !isValidId(fact.getTenantId())
                || Objects.isNull(fact.getAlarmTargetTypeFlag()) || !isValidId(fact.getEntityId())) {
            return null;
        }

        LambdaQueryWrapper<RuleStateDO> wrapper = Wrappers.<RuleStateDO>query().lambda()
                .eq(RuleStateDO::getTenantId, fact.getTenantId())
                .eq(RuleStateDO::getRuleId, match.getRule().getId())
                .eq(RuleStateDO::getAlarmTargetTypeFlag, fact.getAlarmTargetTypeFlag().getIndex())
                .eq(RuleStateDO::getEntityId, fact.getEntityId())
                .eq(RuleStateDO::getStateFlag, RuleStateFlagEnum.FIRING.getIndex())
                .gt(RuleStateDO::getEventId, DEFAULT_ID)
                .orderByDesc(RuleStateDO::getLastTriggerTime)
                .last("limit 1");
        RuleStateDO state = ruleStateManager.getOne(wrapper);
        return Objects.nonNull(state) ? state.getEventId() : null;
    }

    private Long longValue(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof CharSequence text && StringUtils.isNotBlank(text)) {
            try {
                return Long.parseLong(text.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private boolean isValidId(Long id) {
        return Objects.nonNull(id) && id > 0;
    }

}
