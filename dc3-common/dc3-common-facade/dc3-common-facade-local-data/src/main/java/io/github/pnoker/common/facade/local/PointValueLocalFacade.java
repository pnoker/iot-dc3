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

package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointVolumeBO;
import io.github.pnoker.common.facade.local.builder.FacadePointValueBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import reactor.core.publisher.Mono;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;

/**
 * In-process implementation: routes each call straight into {@link PointValueService}.
 * <p>
 * Selected when {@code dc3.facade.data.mode=local}. Carries zero serialization cost — the same
 * JVM handles both caller and service.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointValueLocalFacade implements PointValueFacade {

    private final PointValueService pointValueService;

    private final FacadePointValueBuilder facadePointValueBuilder;

    @Override
    public Mono<FacadePointValueBO> lastValue(Long tenantId, Long deviceId, Long pointId) {
        PointValueQuery query = PointValueQuery.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .pointId(pointId)
                .build();
        return pointValueService.latest(query)
                .flatMap(page -> page.items().stream().findFirst()
                        .map(value -> Mono.just(facadePointValueBuilder.toFacadeBO(value)))
                        .orElseGet(Mono::empty));
    }

    @Override
    public Mono<CursorPage<FacadePointValueBO>> history(Long tenantId, Long deviceId, Long pointId,
                                                        String cursor, int limit) {
        return pointValueService.history(tenantId, deviceId, pointId, cursor, limit)
                .map(result -> CursorPage.of(result.items().stream()
                        .map(facadePointValueBuilder::toFacadeBO).toList(), result.nextCursor()));
    }

    @Override
    public Mono<List<FacadePointVolumeBO>> pointVolumes(Long tenantId, long fromEpochMillis) {
        return pointValueService.seriesVolumes(tenantId, java.time.Instant.ofEpochMilli(fromEpochMillis))
                .map(rows -> rows.stream().map(row -> new FacadePointVolumeBO(row.deviceId(), row.pointId(), row.count())).toList());
    }

}
