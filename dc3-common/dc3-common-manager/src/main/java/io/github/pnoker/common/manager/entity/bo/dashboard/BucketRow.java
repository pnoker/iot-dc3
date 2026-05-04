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

package io.github.pnoker.common.manager.entity.bo.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Generic {@code (key, count)} aggregate row for GROUP BY + COUNT queries.
 * Multiple group columns (enable_flag / driver_type_flag / service_name /
 * driver_id / profile_id) all map to {@code key} via SQL alias so one Row
 * class serves every single-dimension bucket query.
 *
 * <p>Keep distinct from {@link io.github.pnoker.common.manager.entity.vo.dashboard.BucketVO}
 * — VO is the API response shape; this is the mapper result shape. They
 * happen to have identical fields today, but the decoupling lets the API
 * surface evolve independently of the SQL shape.</p>
 */
@Getter
@Setter
@ToString
public class BucketRow {
    /**
     * Group column value. Typed {@code Object} because different queries
     * GROUP BY columns of different JDBC types (SMALLINT enable_flag,
     * VARCHAR service_name, BIGINT driver_id). Service-side formatters
     * narrow it per call-site.
     */
    private Object key;
    private long count;
}
