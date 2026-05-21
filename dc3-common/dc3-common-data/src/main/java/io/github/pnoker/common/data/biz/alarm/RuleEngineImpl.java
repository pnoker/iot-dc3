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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic rule engine implementation.
 *
 * <p>Recovery is treated as a state transition, not an independent match: even
 * when the current fact satisfies the rule's recovery condition, no
 * {@link RuleMatch.recovery} is produced unless an existing FIRING row in
 * {@code dc3_rule_state} attests that the rule actually fired earlier. Without
 * this guard a cold start would emit {@code RECOVERED} notifications for rules
 * that never fired, and any normal data point that happens to satisfy the
 * recovery threshold would do the same.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class RuleEngineImpl implements RuleEngine {

    private final RuleRegistry ruleRegistry;

    private final RuleStateLookup ruleStateLookup;

    private final RuleEvaluator ruleEvaluator;

    @Override
    public List<RuleMatch> evaluate(RuleFact fact) {
        if (Objects.isNull(fact) || Objects.isNull(fact.getTenantId())
                || Objects.isNull(fact.getAlarmTargetTypeFlag())) {
            return List.of();
        }

        List<RuleBO> rules = ruleRegistry.findCandidates(fact);
        List<RuleMatch> matches = new ArrayList<>();
        for (RuleBO rule : rules) {
            if (ruleEvaluator.matches(rule, fact)) {
                matches.add(RuleMatch.firing(rule, fact));
            } else if (ruleEvaluator.recovers(rule, fact) && hasFiringState(rule, fact)) {
                matches.add(RuleMatch.recovery(rule, fact));
            }
        }
        return matches;
    }

    private boolean hasFiringState(RuleBO rule, RuleFact fact) {
        if (Objects.isNull(rule) || Objects.isNull(rule.getId()) || rule.getId() <= 0
                || Objects.isNull(fact.getEntityId()) || fact.getEntityId() <= 0
                || Objects.isNull(rule.getAlarmTargetTypeFlag())) {
            return false;
        }
        return ruleStateLookup.hasFiringState(
                fact.getTenantId(),
                rule.getId(),
                rule.getAlarmTargetTypeFlag().getIndex(),
                fact.getEntityId());
    }

}
