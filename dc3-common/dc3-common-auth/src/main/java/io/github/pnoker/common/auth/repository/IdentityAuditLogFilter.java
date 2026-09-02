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
package io.github.pnoker.common.auth.repository;

import io.github.pnoker.db.r2dbc.core.page.PageRequest;

/** Tenant-scoped filters for identity audit cursor reads. */
public record IdentityAuditLogFilter(
        Long tenantId,
        Long principalId,
        String action,
        String resourceType,
        Long resourceId,
        String status,
        String cursor,
        int limit) {

    public IdentityAuditLogFilter {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenant id is required");
        }
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
    }
}
