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
import io.github.pnoker.common.enums.ApiTypeEnum;
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
 * Query parameters for API listing and filtering.
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
@Schema(description = "API query parameters")
public class ApiQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Pagination parameters including page number, page size, sort order, and time range.")

    private Pages page;

    //

    /**
     * Owning service name, populated by resource registrar
     */
    @Schema(description = "Filter by microservice name (e.g. dc3-center-auth).", example = "dc3-center-manager")
    private String serviceName;

    /**
     * ApiType
     */
    @Schema(description = "HTTP method type of the API endpoint (POST, DELETE, PUT, or GET).", example = "GET")
    private ApiTypeEnum apiTypeFlag;

    /**
     * ApiName
     */
    @Schema(description = "Human-readable display name of the API endpoint.", example = "Add Device")
    private String apiName;

    /**
     * ApiCode, URLMD5
     */
    @Schema(description = "Unique identifier for the API endpoint, derived from the URL MD5 hash.", example = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")
    private String apiCode;

    /**
     * API grouping, usually the owning controller simple class name
     */
    @Schema(description = "Grouping label for the API, typically the simple class name of the owning controller.", example = "DeviceController")
    private String apiGroup;

    /**
     * Enable flag
     */
    @Schema(description = "Enable/disable status of the API; only ENABLE records are accessible at runtime.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
