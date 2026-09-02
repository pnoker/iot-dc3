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
package io.github.pnoker.common.data.service.impl;

import io.github.pnoker.common.data.entity.bo.RuleStateBO;
import io.github.pnoker.common.data.entity.builder.RuleStateBuilder;
import io.github.pnoker.common.data.entity.query.RuleStateQuery;
import io.github.pnoker.common.data.repository.ReactiveRuleStateStore;
import io.github.pnoker.common.data.service.RuleStateService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Reactive rule runtime state service implementation. */
@Service
@RequiredArgsConstructor
public class RuleStateServiceImpl implements RuleStateService {

    private final ReactiveRuleStateStore ruleStateStore;
    private final RuleStateBuilder ruleStateBuilder;

    @Override
    public Mono<RuleStateBO> getById(Long tenantId, Long id) {
        return ruleStateStore
                .get(value(tenantId), value(id))
                .switchIfEmpty(Mono.error(new NotFoundException("Rule state does not exist")))
                .map(ruleStateBuilder::buildBOByDO);
    }

    @Override
    public Mono<OffsetPage<RuleStateBO>> list(Long tenantId, RuleStateQuery query) {
        return Mono.defer(() -> {
            requireTenant(tenantId);
            RuleStateQuery request = Objects.requireNonNullElseGet(query, RuleStateQuery::new);
            PageRequest page = new PageRequest(request.getOffset(), request.getLimit(), request.getSort());
            return ruleStateStore
                    .list(
                            tenantId,
                            request.getRuleId(),
                            index(request.getAlarmTargetTypeFlag()),
                            request.getEntityId(),
                            request.getFingerprint(),
                            index(request.getEntityStateFlag()),
                            request.getAlarmId(),
                            page)
                    .map(result -> OffsetPage.of(
                            result.items().stream()
                                    .map(ruleStateBuilder::buildBOByDO)
                                    .toList(),
                            result.offset(),
                            result.limit(),
                            result.total()));
        });
    }

    @Override
    public Mono<Boolean> delete(Long tenantId, Long id) {
        return ruleStateStore.delete(value(tenantId), value(id));
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private Byte index(io.github.pnoker.common.enums.AlarmTargetTypeEnum value) {
        return value == null ? null : value.getIndex();
    }

    private Byte index(io.github.pnoker.common.enums.RuleStatusEnum value) {
        return value == null ? null : value.getIndex();
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId is required");
    }
}
