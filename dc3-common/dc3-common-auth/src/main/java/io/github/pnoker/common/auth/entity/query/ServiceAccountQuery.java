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
import io.github.pnoker.common.enums.EnableFlagEnum;
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
 * Query parameters for service accounts.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service account query parameters")
public class ServiceAccountQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination parameters including page number, page size, time range, and sort order")
    private Pages page;

    @Schema(description = "Tenant identifier used to scope the query; only service accounts belonging to this tenant are returned.", example = "1024")
    private Long tenantId;

    @Schema(description = "Identifier of the principal (user or service account) performing the query; used to restrict results to accounts accessible by this principal.", example = "2048")
    private Long principalId;

    @Schema(description = "Partial or full name of the service account to search for; supports fuzzy matching.", example = "mqtt-broker-account")
    private String serviceAccountName;

    @Schema(description = "Identifier of the owner principal; filters service accounts that belong to the specified owner within the current tenant.", example = "3072")
    private Long ownerPrincipalId;

    @Schema(description = "Enable/disable status filter; when specified, only service accounts with the matching status are returned.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
