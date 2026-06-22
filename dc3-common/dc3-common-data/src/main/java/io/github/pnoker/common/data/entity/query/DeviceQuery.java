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

package io.github.pnoker.common.data.entity.query;

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
 * Query parameters for device listing and filtering.
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
@Schema(description = "Device query parameters")
public class DeviceQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination parameters including page number, page size, sort order, and time range.")
    private Pages page;

    /**
     * Tenant ID
     */
    @Schema(description = "Tenant ID for multi-tenant isolation. Required for query scope.")
    private Long tenantId;

    //

    /**
     * Device Name
     */
    @Schema(description = "Filter by device name. Supports partial matching.", example = "Temperature Sensor 01")
    private String deviceName;

    /**
     * Device ID
     */
    @Schema(description = "Filter by device code. Exact match on the stable business identifier.", example = "DEV_0001")
    private String deviceCode;

    /**
     * Driver ID
     */
    @Schema(description = "Filter by driver ID. Returns results scoped to a specific driver.", example = "1024")
    private Long driverId;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1).", example = "ENABLE")
    private EnableFlagEnum enableFlag;

    /**
     *
     */
    @Schema(description = "Optimistic-lock version number for concurrent update control.", example = "1")
    private Integer version;

    //

    /**
     * ID
     */
    @Schema(description = "Filter by profile (device template) ID.", example = "2048")
    private Long profileId;

}
