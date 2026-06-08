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

import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Query parameters for point listing and filtering.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Point query parameters")
public class PointQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination object")

    private Pages page;

    /**
     * Tenant ID
     */
    @Schema(description = "Tenant ID")
    private Long tenantId;

    //

    /**
     * Name
     */
    @Schema(description = "point name")
    private String pointName;

    /**
     * Code
     */
    @Schema(description = "point code")
    private String pointCode;

    /**
     * Type
     */
    @Schema(description = "point type flag")
    private PointTypeFlagEnum pointTypeFlag;

    /**
     *
     */
    @Schema(description = "rw flag")
    private RwFlagEnum rwFlag;

    /**
     * ID
     */
    @Schema(description = "profile ID")
    private Long profileId;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: 0=enabled, 1=disabled")
    private EnableFlagEnum enableFlag;

    /**
     * Group ID
     */
    @Schema(description = "group ID")
    private Long groupId;

    /**
     * Label ID
     */
    @Schema(description = "label ID")
    private Long labelId;

    /**
     *
     */
    @Schema(description = "Version number")
    private Integer version;

    //

    /**
     * Device ID
     */
    @Schema(description = "device ID")
    private Long deviceId;

}
