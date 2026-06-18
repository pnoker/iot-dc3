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
import io.github.pnoker.common.enums.MembershipStatusEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
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
 * Query parameters for tenant memberships.
 *
 * @author pnoker
 * @version 2026.6.13
 * @since 2026.6.13
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tenant membership query parameters")
public class TenantMembershipQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination parameters (page number and page size).")
    private Pages page;

    @Schema(description = "Filter by tenant identifier; restricts results to memberships belonging to the specified tenant.", example = "1024")
    private Long tenantId;

    @Schema(description = "Filter by principal identifier (user, service account, or system identity) within the tenant.", example = "2048")
    private Long principalId;

    @Schema(description = "Filter by principal classification: USER, SERVICE_ACCOUNT, or SYSTEM.", example = "USER")
    private PrincipalTypeEnum principalType;

    @Schema(description = "Filter by membership lifecycle status: ACTIVE, SUSPENDED, or INVITED.", example = "ACTIVE")
    private MembershipStatusEnum membershipStatus;
}
