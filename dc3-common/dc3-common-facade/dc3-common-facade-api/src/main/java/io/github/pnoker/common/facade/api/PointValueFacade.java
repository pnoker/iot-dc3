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
package io.github.pnoker.common.facade.api;

import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointVolumeBO;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Protocol-neutral point value facade.
 * <p>
 * Mirrors the value-query RPCs on {@code api.center.data.PointValueApi} but returns plain
 * BO types so callers never have to touch gRPC or protobuf classes. Two implementations
 * back this interface:
 * <ul>
 * <li>{@code PointValueLocalFacade} — in-process call into {@code PointValueService},
 * selected when {@code dc3.facade.data.mode=local}.</li>
 * <li>{@code PointValueGrpcFacade} — gRPC call against Data Center, selected when
 * {@code dc3.facade.data.mode=grpc} (default).</li>
 * </ul>
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface PointValueFacade {

    /** Load the latest point value for the series. */
    Mono<FacadePointValueBO> lastValue(Long tenantId, Long deviceId, Long pointId);

    /** Load the cursor-paged history for the series. */
    Mono<CursorPage<FacadePointValueBO>> history(Long tenantId, Long deviceId, Long pointId, String cursor, int limit);

    /** Load point value volumes per point since the epoch instant. */
    Mono<List<FacadePointVolumeBO>> pointVolumes(Long tenantId, long fromEpochMillis);
}
