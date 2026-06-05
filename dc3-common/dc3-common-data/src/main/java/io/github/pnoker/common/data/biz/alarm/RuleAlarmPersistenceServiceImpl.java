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

import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.service.AlarmConstant;
import io.github.pnoker.common.data.dal.EntityAlarmManager;
import io.github.pnoker.common.data.entity.model.EntityAlarmDO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.RuleAlarmEventExt;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.enums.AlarmSourceFlagEnum;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Rule alarm persistence service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleAlarmPersistenceServiceImpl implements RuleAlarmPersistenceService {

    private final EntityAlarmManager entityAlarmManager;

    private final RuleStateLookup ruleStateLookup;

    @Override
    public void ensureAlarm(RuleMatch match) {
        if (Objects.isNull(match) || Objects.isNull(match.getRule()) || Objects.isNull(match.getFact())) {
            return;
        }
        RuleFact fact = match.getFact();
        if (isValidId(fact.getAlarmId())) {
            return;
        }

        Long firingAlarmId = getFiringAlarmId(match);
        if (isValidId(firingAlarmId)) {
            fact.setAlarmId(firingAlarmId);
            return;
        }
        if (!StringUtils.equalsIgnoreCase(AlarmConstant.MATCH_TYPE_FIRING, match.getMatchType())) {
            return;
        }

        Long alarmId = persistEntityAlarm(match);
        if (isValidId(alarmId)) {
            fact.setAlarmId(alarmId);
        }
    }

    private Long persistEntityAlarm(RuleMatch match) {
        RuleFact fact = match.getFact();
        AlarmTargetTypeFlagEnum targetType = fact.getAlarmTargetTypeFlag();
        Long entityId = fact.getEntityId();

        if (!isValidId(entityId)) {
            log.warn("Skip rule entity alarm because entityId is missing, ruleId={}, targetType={}",
                    match.getRule().getId(), targetType);
            return null;
        }

        Long driverId = longValue(fact.value("driverId"));
        Long deviceId = longValue(fact.value("deviceId"));
        Long pointId = AlarmTargetTypeFlagEnum.POINT.equals(targetType) ? entityId
                : longValue(fact.value("pointId"));

        if (AlarmTargetTypeFlagEnum.DEVICE.equals(targetType) || AlarmTargetTypeFlagEnum.POINT.equals(targetType)) {
            if (!isValidId(deviceId)) {
                deviceId = AlarmTargetTypeFlagEnum.DEVICE.equals(targetType) ? entityId : DefaultConstant.DEFAULT_ID;
            }
        }

        EntityAlarmDO entity = new EntityAlarmDO();
        entity.setAlarmTargetTypeFlag(targetType.getIndex());
        entity.setEntityId(entityId);
        entity.setDriverId(Objects.requireNonNullElse(driverId, DefaultConstant.DEFAULT_ID));
        entity.setDeviceId(Objects.requireNonNullElse(deviceId, DefaultConstant.DEFAULT_ID));
        entity.setPointId(Objects.requireNonNullElse(pointId, DefaultConstant.DEFAULT_ID));
        entity.setRuleId(match.getRule().getId());
        entity.setAlarmTypeFlag(AlarmTypeFlagEnum.RULE.getIndex());
        entity.setAlarmSourceFlag(AlarmSourceFlagEnum.RULE.getIndex());
        // Severity originates from rule_ext.severity — message_level on the
        // template is for rendering only, not for level routing. Default to P2
        // when the rule does not declare a severity.
        entity.setAlarmLevelFlag(AlarmLevelResolver.resolve(match.getSeverity(), AlarmMessageLevelFlagEnum.P2)
                .getIndex());
        entity.setAlarmExt(alarmExt(match));
        entity.setExpiredTime(DefaultConstant.DEFAULT_ID);
        entity.setConfirmFlag((byte) 0);
        entity.setTenantId(Objects.requireNonNullElse(fact.getTenantId(), DefaultConstant.DEFAULT_ID));
        if (!entityAlarmManager.save(entity)) {
            log.warn("Failed to persist entity alarm, ruleId={}, targetType={}, entityId={}",
                    match.getRule().getId(), targetType, entityId);
            return null;
        }
        return entity.getId();
    }

    private JsonExt alarmExt(RuleMatch match) {
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

    private Long getFiringAlarmId(RuleMatch match) {
        RuleFact fact = match.getFact();
        if (!isValidId(match.getRule().getId()) || !isValidId(fact.getTenantId())
                || Objects.isNull(fact.getAlarmTargetTypeFlag()) || !isValidId(fact.getEntityId())) {
            return null;
        }
        return ruleStateLookup.getFiringAlarmId(
                fact.getTenantId(),
                match.getRule().getId(),
                fact.getAlarmTargetTypeFlag().getIndex(),
                fact.getEntityId());
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
