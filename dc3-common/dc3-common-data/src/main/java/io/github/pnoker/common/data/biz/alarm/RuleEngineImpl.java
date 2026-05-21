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

import io.github.pnoker.common.data.dal.RuleManager;
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.builder.RuleBuilder;
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic rule engine implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class RuleEngineImpl implements RuleEngine {

    private static final long GLOBAL_ENTITY_ID = 0L;

    private final RuleManager ruleManager;

    private final RuleBuilder ruleBuilder;

    private final RuleEvaluator ruleEvaluator;

    @Override
    public List<RuleMatch> evaluate(RuleFact fact) {
        if (Objects.isNull(fact) || Objects.isNull(fact.getTenantId())
                || Objects.isNull(fact.getAlarmTargetTypeFlag())) {
            return List.of();
        }

        List<RuleBO> rules = ruleBuilder.buildBOListByDOList(loadCandidateRules(fact));
        List<RuleMatch> matches = new ArrayList<>();
        for (RuleBO rule : rules) {
            if (ruleEvaluator.matches(rule, fact)) {
                matches.add(RuleMatch.firing(rule, fact));
            } else if (ruleEvaluator.recovers(rule, fact)) {
                matches.add(RuleMatch.recovery(rule, fact));
            }
        }
        return matches;
    }

    private List<RuleDO> loadCandidateRules(RuleFact fact) {
        return ruleManager.lambdaQuery()
                .eq(RuleDO::getTenantId, fact.getTenantId())
                .eq(RuleDO::getAlarmTargetTypeFlag, fact.getAlarmTargetTypeFlag().getIndex())
                .eq(RuleDO::getEnableFlag, EnableFlagEnum.ENABLE.getIndex())
                .and(Objects.nonNull(fact.getEntityId()),
                        wrapper -> wrapper.eq(RuleDO::getEntityId, fact.getEntityId())
                                .or()
                                .eq(RuleDO::getEntityId, GLOBAL_ENTITY_ID))
                .list();
    }

}
