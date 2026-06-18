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

import io.github.pnoker.common.entity.common.Pages;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * Query parameters for identity audit log entries.
 *
 * @author pnoker
 * @version 2026.6.14
 * @since 2026.6.14
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Identity audit log query parameters")
public class IdentityAuditLogQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination parameters including page number, page size, and time-range filters.")
    private Pages page;

    @Schema(description = "Tenant identifier; restricts audit log results to records belonging to this tenant.", example = "1024")
    private Long tenantId;

    @Schema(description = "Identifier of the principal (user or service account) whose audit log entries are queried; must belong to the current tenant.", example = "2048")
    private Long principalId;

    @Schema(description = "Audit action type to filter by, e.g. LOGIN, LOGOUT, CREATE, UPDATE, DELETE.", example = "LOGIN")
    private String action;

    @Schema(description = "Resource type to filter by, identifying the category of the audited resource, e.g. USER, ROLE, DEVICE.", example = "USER")
    private String resourceType;

    @Schema(description = "Identifier of the specific resource involved in the audited operation.", example = "4096")
    private Long resourceId;

    @Schema(description = "Outcome status of the audited operation to filter by, e.g. SUCCESS, FAILURE.", example = "SUCCESS")
    private String status;
}
