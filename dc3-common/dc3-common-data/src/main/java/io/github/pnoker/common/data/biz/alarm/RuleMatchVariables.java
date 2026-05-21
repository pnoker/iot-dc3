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

import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.entity.ext.RuleExt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Template variables exposed by a rule match.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
final class RuleMatchVariables {

    private RuleMatchVariables() {
    }

    static Map<String, Object> of(RuleMatch match) {
        RuleBO rule = match.getRule();
        RuleFact fact = match.getFact();
        Map<String, Object> factValues = Objects.requireNonNullElse(fact.getValues(), Map.of());
        Map<String, Object> variables = RuleValueMap.from(new Snapshot(
                fact.getTenantId(),
                rule.getId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                fact.getEntityId(),
                rule.getAlarmTargetTypeFlag().getCode(),
                fact.getAlarmId(),
                fact.getFactTime(),
                fact.getFactTime(),
                match.getMatchType(),
                match.getSeverity(),
                match.getEventType(),
                match.getLabels(),
                factValues));
        variables.putAll(factValues);
        enrichRuleConditionVariables(rule, variables);
        return variables;
    }

    private static void enrichRuleConditionVariables(RuleBO rule, Map<String, Object> variables) {
        if (Objects.isNull(rule.getRuleExt()) || Objects.isNull(rule.getRuleExt().getContent())
                || Objects.isNull(rule.getRuleExt().getContent().getCondition())) {
            return;
        }
        RuleExt.Condition condition = rule.getRuleExt().getContent().getCondition();
        variables.putIfAbsent(Variable.VALUE, variables.get(condition.getField()));
        variables.putIfAbsent(Variable.POINT, variables.getOrDefault(Variable.POINT_NAME, rule.getEntityId()));
        variables.putIfAbsent(Variable.DEVICE, variables.getOrDefault(Variable.DEVICE_NAME, rule.getEntityId()));
        variables.putIfAbsent(Variable.THRESHOLD, condition.getThreshold());
        variables.putIfAbsent(Variable.LOW, condition.getLow());
        variables.putIfAbsent(Variable.HIGH, condition.getHigh());
        variables.putIfAbsent(Variable.UNIT, condition.getUnit());
    }

    private record Snapshot(
            Long tenantId,
            Long ruleId,
            String ruleCode,
            String ruleName,
            Long entityId,
            String alarmTargetType,
            Long alarmId,
            LocalDateTime factTime,
            LocalDateTime triggerTime,
            String matchType,
            String severity,
            String eventType,
            List<String> labels,
            Map<String, Object> values) {
    }

    private static final class Variable {

        private static final String VALUE = "value";
        private static final String POINT = "point";
        private static final String POINT_NAME = "pointName";
        private static final String DEVICE = "device";
        private static final String DEVICE_NAME = "deviceName";
        private static final String THRESHOLD = "threshold";
        private static final String LOW = "low";
        private static final String HIGH = "high";
        private static final String UNIT = "unit";

        private Variable() {
        }

    }

}
