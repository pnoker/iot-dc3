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
import io.github.pnoker.common.enums.ExpireTypeEnum;
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
 * Query parameters for driver token listing and filtering.
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
@Schema(description = "Driver Token query parameters")
public class DriverTokenQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination parameters including page number, page size, sort order, and time range.")
    private Pages page;

    /**
     * Tenant ID
     */
    @Schema(description = "Tenant identifier used to scope all query results; only driver tokens belonging to this tenant are returned.", example = "1024")
    private Long tenantId;

    /**
     * Driver ID
     */
    @Schema(description = "Filter by driver code. Exact match on the stable business identifier.", example = "dc3-driver-modbus-tcp")
    private String driverCode;

    /**
     * AppID
     */
    @Schema(description = "Application identifier of the driver; used to filter tokens issued to a specific driver application.", example = "dc3-app-modbus")
    private String driverAppId;

    /**
     *
     */
    @Schema(description = "Filter by token expiration type; limits results to tokens with the specified validity period.", example = "PERMANENT")
    private ExpireTypeEnum expireFlag;

    /**
     * Enable flag
     */
    @Schema(description = "Filter by token enabled status; returns only tokens that are enabled or disabled.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
