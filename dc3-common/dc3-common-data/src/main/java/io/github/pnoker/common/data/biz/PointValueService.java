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
package io.github.pnoker.common.data.biz;

import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.bo.PointValueVolumeBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.db.r2dbc.core.page.CursorPage;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Business service for point value operations.
 *
 * @author pnoker
 * @since 2016.10.1
 */
public interface PointValueService {

    /**
     * Save point value
     *
     * @param pointValueBO PointValue
     */
    Mono<Void> save(PointValueBO pointValueBO);

    /**
     * Batch save point values
     *
     * @param pointValueBOList Array
     */
    Mono<Void> save(List<PointValueBO> pointValueBOList);

    /**
     * Get historical point values within the tenant scope.
     *
     * @param tenantId Tenant ID (required for tenant isolation)
     * @param deviceId Device ID
     * @param pointId  Point ID
     * @param cursor   Opaque cursor returned by the previous page
     * @param limit    Number of values to retrieve
     * @return History values (each with create_time), newest first
     */
    Mono<CursorPage<PointValueBO>> history(Long tenantId, Long deviceId, Long pointId, String cursor, int limit);

    /**
     * Get latest point values with pagination and sorting
     *
     * @param pointValueQuery Entry of Query
     * @return Entity of BO Page
     */
    Mono<OffsetPage<PointValueBO>> latest(PointValueQuery pointValueQuery);

    /**
     * Get point values with pagination and sorting
     *
     * @param pointValueQuery Entry of Query
     * @return Entity of BO Page
     */
    Mono<CursorPage<PointValueBO>> page(PointValueQuery pointValueQuery);

    /**
     * Per-series sample volumes since a lower bound (dashboard topology
     * weights). One row per (device, point) pair with samples in the window.
     *
     * @param tenantId owning tenant
     * @param from     inclusive lower bound, absolute instant
     * @return the volume rows, never {@code null}
     */
    Mono<List<PointValueVolumeBO>> seriesVolumes(Long tenantId, Instant from);
}
