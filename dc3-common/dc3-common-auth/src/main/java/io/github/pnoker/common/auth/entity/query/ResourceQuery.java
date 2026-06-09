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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "Pagination object")

    private Pages page;

    //

    /**
     * Name
     */
    @Schema(description = "Resource name")
    private String resourceName;

    /**
     * Code
     */
    @Schema(description = "Resource permission code")
    private String resourceCode;

    /**
     * Type
     */
    @Schema(description = "Resource type flag", example = "MENU")
    private ResourceTypeEnum resourceTypeFlag;

    /**
     * Type multi-select — takes precedence over {@link #resourceTypeFlag} when non-empty.
     */
    @Schema(description = "resource type flags")
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
    @Schema(description = "Resource scope flag", example = "LIST")
    private ResourceScopeTypeEnum resourceScopeFlag;

    /**
     * Scope multi-select — takes precedence over {@link #resourceScopeFlag} when
     * non-empty.
     */
    @Schema(description = "resource scope flags")
    private List<ResourceScopeTypeEnum> resourceScopeFlags;

    /**
     * Parent resource id filter (optional).
     */
    @Schema(description = "Parent resource ID")
    private Long parentResourceId;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: 0=enabled, 1=disabled", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
