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
import io.github.pnoker.common.data.dal.RuleStateManager;
import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.common.enums.RuleStateFlagEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Read-only helper that exposes the small subset of {@code dc3_rule_state}
 * queries the rule engine relies on, separated out so unit tests can mock the
 * lookup without standing up a full MyBatis-Plus chain wrapper.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.21
 */
@Component
@RequiredArgsConstructor
public class RuleStateLookup {

    private final RuleStateManager ruleStateManager;

    /**
     * Returns true if at least one row exists for the given rule and entity that
     * is currently in the {@code FIRING} state. Fingerprint is intentionally not
     * part of the predicate — fingerprints are only resolved at notification
     * time and any firing fingerprint is sufficient to authorize a recovery.
     */
    public boolean hasFiringState(long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId) {
        return ruleStateManager.lambdaQuery()
                .eq(RuleStateDO::getTenantId, tenantId)
                .eq(RuleStateDO::getRuleId, ruleId)
                .eq(RuleStateDO::getAlarmTargetTypeFlag, alarmTargetTypeFlag)
                .eq(RuleStateDO::getEntityId, entityId)
                .eq(RuleStateDO::getStateFlag, RuleStateFlagEnum.FIRING.getIndex())
                .exists();
    }

    /**
     * Returns the most recent {@code event_id} (i.e. the {@code dc3_entity_alarm.id})
     * recorded on a {@code FIRING} row for this rule + entity, or {@code null}
     * when none exists. Used by the alarm pipeline to reuse an existing firing
     * alarm id on a follow-up evaluation rather than creating a duplicate row.
     */
    public Long findFiringEventId(long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId) {
        RuleStateDO state = ruleStateManager.lambdaQuery()
                .eq(RuleStateDO::getTenantId, tenantId)
                .eq(RuleStateDO::getRuleId, ruleId)
                .eq(RuleStateDO::getAlarmTargetTypeFlag, alarmTargetTypeFlag)
                .eq(RuleStateDO::getEntityId, entityId)
                .eq(RuleStateDO::getStateFlag, RuleStateFlagEnum.FIRING.getIndex())
                .gt(RuleStateDO::getEventId, DefaultConstant.DEFAULT_ID)
                .orderByDesc(RuleStateDO::getLastTriggerTime)
                .last("limit 1")
                .one();
        return Objects.nonNull(state) ? state.getEventId() : null;
    }

}
