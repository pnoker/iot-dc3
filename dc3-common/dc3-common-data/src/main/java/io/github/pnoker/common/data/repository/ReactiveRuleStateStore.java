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

import io.github.pnoker.common.data.entity.model.RuleStateDO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import java.time.LocalDateTime;
import reactor.core.publisher.Mono;

/** Reactive tenant-scoped persistence for rule runtime state. */
public interface ReactiveRuleStateStore {

    Mono<RuleStateDO> get(long tenantId, long stateId);

    Mono<OffsetPage<RuleStateDO>> list(
            long tenantId,
            Long ruleId,
            Byte alarmTargetTypeFlag,
            Long entityId,
            String fingerprint,
            Byte entityStateFlag,
            Long alarmId,
            PageRequest page);

    Mono<Boolean> delete(long tenantId, long stateId);

    Mono<RuleStateDO> find(long tenantId, long ruleId, byte alarmTargetTypeFlag, long entityId, String fingerprint);

    Mono<RuleStateDO> transition(RuleStateDO state, boolean recovery);

    Mono<Boolean> updateLastNotifyTime(long tenantId, long stateId, LocalDateTime lastNotifyTime);
}
