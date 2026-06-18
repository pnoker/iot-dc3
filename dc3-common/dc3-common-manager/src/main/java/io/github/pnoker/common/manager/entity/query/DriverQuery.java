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
import io.github.pnoker.common.enums.DriverTypeEnum;
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
 * Query parameters for driver listing and filtering.
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
@Schema(description = "Driver query parameters")
public class DriverQuery implements Serializable {

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
     * Name
     */
    @Schema(description = "Filter by driver name. Supports partial matching.", example = "Modbus TCP")
    private String driverName;

    /**
     * Driver ID
     */
    @Schema(description = "Filter by driver code. Exact match on the stable business identifier.", example = "dc3-driver-modbus-tcp")
    private String driverCode;

    /**
     * Driver service name
     */
    @Schema(description = "Filter by microservice name (e.g. dc3-center-auth).", example = "dc3-center-manager")
    private String serviceName;

    /**
     *
     */
    @Schema(description = "Filter by driver service host (IPv4 address).", example = "192.168.1.100")
    private String serviceHost;

    /**
     * Type
     */
    @Schema(description = "Filter by driver type: DRIVER_CLIENT or DRIVER_SERVER.", example = "DRIVER_SERVER")
    private DriverTypeEnum driverTypeFlag;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1).", example = "ENABLE")
    private EnableFlagEnum enableFlag;

    /**
     * Group ID
     */
    @Schema(description = "Filter by group ID to restrict results to entities in a specific group.", example = "4096")
    private Long groupId;

    /**
     * Label ID
     */
    @Schema(description = "Filter by label ID to restrict results to entities tagged with a specific label.", example = "2048")
    private Long labelId;

    /**
     *
     */
    @Schema(description = "Optimistic-lock version number for concurrent update control.", example = "1")
    private Integer version;

}
