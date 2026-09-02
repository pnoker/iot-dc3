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
package io.github.pnoker.common.auth.entity.query;

import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;

/** Canonical identity-audit request using signed cursor pagination. */
@Schema(description = "Identity audit list request using a signed cursor")
public record IdentityAuditLogCursorRequest(
        @Schema(description = "Opaque signed cursor from the previous page")
        String cursor,

        @Schema(description = "Maximum number of records") Integer limit,
        @Schema(description = "Principal identifier filter") Long principalId,
        @Schema(description = "Action filter") String action,
        @Schema(description = "Resource type filter") String resourceType,
        @Schema(description = "Resource identifier filter") Long resourceId,
        @Schema(description = "Outcome status filter") String status) {

    public IdentityAuditLogCursorRequest {
        limit = limit == null ? PageRequest.DEFAULT_LIMIT : limit;
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + PageRequest.MAX_LIMIT);
        }
    }

    public IdentityAuditLogCursorRequest() {
        this(null, PageRequest.DEFAULT_LIMIT, null, null, null, null, null);
    }
}
