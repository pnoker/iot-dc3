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
import io.github.pnoker.common.enums.ResourceScopeTypeEnum;
import io.github.pnoker.common.enums.ResourceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Query parameters for resource listing and filtering.
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
@Schema(description = "Resource query parameters")
public class ResourceQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination parameters including page number, page size, sort order, and time range.")
    private Pages page;

    //

    /**
     * Name
     */
    @Schema(description = "Partial or exact name of the resource used for filtering; case-insensitive match.", example = "Device Management")
    private String resourceName;

    /**
     * Code
     */
    @Schema(description = "Unique permission code of the resource used for filtering; must match the registered code exactly.", example = "device:list")
    private String resourceCode;

    /**
     * Type
     */
    @Schema(description = "Single resource type filter; ignored when resourceTypeFlags is non-empty.", example = "MENU")
    private ResourceTypeEnum resourceTypeFlag;

    /**
     * Type multi-select — takes precedence over {@link #resourceTypeFlag} when non-empty.
     */
    @Schema(description = "Multi-value resource type filter; when non-empty takes precedence over resourceTypeFlag. Allowed values: DRIVER, PROFILE, POINT, DEVICE, DATA, MENU, API.")
    private List<ResourceTypeEnum> resourceTypeFlags;

    /**
     * , : ResourceScopeTypeEnum
     * <ul>
     * <li>0x01:</li>
     * <li>0x02:</li>
     * <li>0x04:</li>
     * <li>0x08:</li>
     * </ul>
     *
     */
    @Schema(description = "Single resource scope type filter; ignored when resourceScopeFlags is non-empty.", example = "LIST")
    private ResourceScopeTypeEnum resourceScopeFlag;

    /**
     * Scope multi-select — takes precedence over {@link #resourceScopeFlag} when
     * non-empty.
     */
    @Schema(description = "Multi-value resource scope filter; when non-empty takes precedence over resourceScopeFlag. Allowed values: ADD, DELETE, UPDATE, LIST, GET.")
    private List<ResourceScopeTypeEnum> resourceScopeFlags;

    /**
     * Parent resource id filter (optional).
     */
    @Schema(description = "ID of the parent resource; filters results to direct children of this resource node.", example = "1024")
    private Long parentResourceId;

    /**
     * Enable flag
     */
    @Schema(description = "Enable status of the resource; filters to only enabled or only disabled resources.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
