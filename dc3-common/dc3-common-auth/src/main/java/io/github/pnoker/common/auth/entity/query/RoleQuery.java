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
 * Query parameters for role listing and filtering.
 *
 * @author linys
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role query parameters")
public class RoleQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination parameters including page number, page size, sort order, and time range.")
    private Pages page;

    /**
     * Tenant ID
     */
    @Schema(description = "Tenant identifier used to scope this query; only roles belonging to this tenant are returned.", example = "1024")
    private Long tenantId;

    //

    /**
     * Name
     */
    @Schema(description = "Role display name to filter by; supports partial (fuzzy) matching.", example = "Administrator")
    private String roleName;

    /**
     * Code
     */
    @Schema(description = "Unique role code to filter by; exact match against the role's business identifier.", example = "ROLE_ADMIN")
    private String roleCode;

    /**
     * Enable flag
     */
    @Schema(description = "Activation status of the role; ENABLE returns only active roles, DISABLE returns only inactive roles.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
