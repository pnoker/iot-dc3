/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
