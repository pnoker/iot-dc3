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

import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.bo.WindowAggregateResult;
import io.github.pnoker.common.entity.query.WindowAggregateRequest;
import io.github.pnoker.common.enums.AlarmTargetTypeFlagEnum;
import io.github.pnoker.common.enums.WindowMode;
import io.github.pnoker.common.repository.RepositoryService;
import io.github.pnoker.common.strategy.RepositoryStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Long-window backend. Pushes the aggregate to the time-series repository
 * (PostgreSQL today) so the alarm engine doesn't have to materialize an
 * unbounded sample list in memory. ALL/ANY still pull raw rows here because
 * the rule's condition is sample-by-sample.
 *
 * <p>Only POINT facts are supported — the time-series store is keyed on
 * (tenantId, deviceId, pointId). Device/driver windowed alarms over long
 * spans aren't a current use case; supporting them would require a different
 * storage layout.
 *
 * @author pnoker
 * @version 2026.5.21
 * @since 2026.5.21
 */
@Slf4j
@Component
public class RepositoryWindowDataSource implements WindowDataSource {

    @Override
    public AggregateOutcome aggregate(WindowSpec spec, RuleFact fact, WindowMode mode) {
        if (!isPointFact(fact) || Objects.isNull(spec) || Objects.isNull(spec.duration())) {
            return AggregateOutcome.empty();
        }
        Long deviceId = longValue(fact.value("deviceId"));
        if (Objects.isNull(deviceId) || deviceId <= 0) {
            return AggregateOutcome.empty();
        }
        LocalDateTime to = Objects.requireNonNullElse(fact.getFactTime(), LocalDateTime.now());
        LocalDateTime from = to.minus(spec.duration());

        WindowAggregateRequest req = WindowAggregateRequest.builder()
                .tenantId(fact.getTenantId())
                .deviceId(deviceId)
                .pointId(fact.getEntityId())
                .function(mode.name())
                .from(from)
                .to(to)
                .build();
        try {
            WindowAggregateResult result = currentRepository().aggregateInWindow(req);
            return new AggregateOutcome(result.value(), result.sampleCount());
        } catch (RuntimeException e) {
            log.warn("Repository window aggregate failed, treating as empty; tenantId={}, pointId={}, mode={}",
                    fact.getTenantId(), fact.getEntityId(), mode, e);
            return AggregateOutcome.empty();
        }
    }

    @Override
    public List<WindowSample> samples(WindowSpec spec, RuleFact fact) {
        if (!isPointFact(fact) || Objects.isNull(spec) || Objects.isNull(spec.duration())) {
            return List.of();
        }
        Long deviceId = longValue(fact.value("deviceId"));
        if (Objects.isNull(deviceId) || deviceId <= 0) {
            return List.of();
        }
        LocalDateTime to = Objects.requireNonNullElse(fact.getFactTime(), LocalDateTime.now());
        LocalDateTime from = to.minus(spec.duration());
        try {
            List<PointValueBO> rows = currentRepository().samplesInWindow(
                    fact.getTenantId(), deviceId, fact.getEntityId(), from, to);
            return rows.stream()
                    .map(bo -> new WindowSample(bo.getNumValue(), bo.getCalValue(), bo.getCreateTime()))
                    .toList();
        } catch (RuntimeException e) {
            log.warn("Repository window samples failed; tenantId={}, pointId={}",
                    fact.getTenantId(), fact.getEntityId(), e);
            return List.of();
        }
    }

    private static boolean isPointFact(RuleFact fact) {
        return Objects.nonNull(fact) && fact.getAlarmTargetTypeFlag() == AlarmTargetTypeFlagEnum.POINT
                && Objects.nonNull(fact.getTenantId()) && Objects.nonNull(fact.getEntityId());
    }

    private static Long longValue(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * Look the strategy up dynamically — keeps this class compatible with the
     * single-repository constraint enforced elsewhere without coupling to a
     * specific implementation.
     */
    private RepositoryService currentRepository() {
        List<RepositoryService> impls = RepositoryStrategyFactory.get();
        if (impls.isEmpty()) {
            throw new IllegalStateException("No repository service registered");
        }
        return impls.get(0);
    }

}
