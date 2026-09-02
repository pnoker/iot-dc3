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
package io.github.pnoker.common.data.repository;

import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped persistence port for alarm rules. */
public interface ReactiveRuleStore {

    Flux<RuleBO> listEnabledCandidates(long tenantId, AlarmTargetTypeEnum targetType, long entityId);

    Mono<RuleDO> get(long tenantId, long id);

    Mono<OffsetPage<RuleDO>> list(
            long tenantId,
            String ruleName,
            String ruleCode,
            Long entityId,
            AlarmTargetTypeEnum targetType,
            io.github.pnoker.common.enums.EnableFlagEnum enableFlag,
            PageRequest pageRequest);

    Mono<RuleDO> insert(RuleDO rule);

    Mono<RuleDO> update(RuleDO rule);

    Mono<Boolean> softDelete(long tenantId, long id);

    Mono<Boolean> hasChildren(long tenantId, long id);

    Mono<Boolean> existsActiveCode(long tenantId, String ruleCode, Long excludedId);
}
