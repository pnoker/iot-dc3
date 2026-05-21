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
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the {@code dc3_rule} query used by the rule engine to fetch
 * candidate rules for a fact, kept separate so unit tests can stub the result
 * list without standing up MyBatis-Plus chain wrappers.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.21
 */
@Component
@RequiredArgsConstructor
public class RuleCandidateLookup {

    private static final long GLOBAL_ENTITY_ID = 0L;

    private final RuleManager ruleManager;

    /**
     * Returns enabled rules for the given fact: those bound to the same entity
     * id, plus tenant-global rules ({@code entity_id = 0}). Result preserves the
     * default insertion order from the underlying query.
     */
    public List<RuleDO> findCandidates(RuleFact fact) {
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
