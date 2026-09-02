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
package io.github.pnoker.common.manager.entity.query;

import io.github.pnoker.db.r2dbc.core.page.PageRequest;

/** Canonical offset query for topic projections. */
public record TopicOffsetQuery(Long tenantId, String topic, String deviceName, long offset, int limit) {
    public TopicOffsetQuery {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        if (offset < 0 || limit < 1 || limit > PageRequest.MAX_LIMIT)
            throw new IllegalArgumentException("invalid page bounds");
    }
}
