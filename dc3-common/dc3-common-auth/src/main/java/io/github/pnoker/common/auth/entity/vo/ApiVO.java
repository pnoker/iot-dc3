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

package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.ApiExt;
import io.github.pnoker.common.enums.ApiTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Auth;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for API API responses.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "API view object")
public class ApiVO extends BaseVO {

    /**
     * Owning service name, populated by resource registrar
     */
    @Schema(description = "Microservice name that owns this API endpoint (e.g. dc3-center-auth).", example = "dc3-center-auth")
    private String serviceName;

    /**
     * ApiType
     */
    @Schema(description = "HTTP method type of this API endpoint (REST verb classification).", example = "GET")
    private ApiTypeEnum apiTypeFlag;

    /**
     * ApiName
     */
    @NotBlank(message = "API name can't be empty", groups = {Add.class, Auth.class})
    @Schema(description = "API endpoint display name. Unique name within a tenant.", example = "Get User by ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid API name", groups = {Add.class, Update.class})
    private String apiName;

    /**
     * ApiCode, URLMD5
     */
    @Schema(description = "API code. Stable business identifier used for permission matching.", example = "user_get_by_id")
    private String apiCode;

    /**
     * API grouping, usually the owning controller simple class name
     */
    @Schema(description = "API group label for organizing endpoints into logical categories in the UI.", example = "User Management")
    private String apiGroup;

    /**
     * Api
     */
    @Schema(description = "API extension information, serialized as JSON for custom endpoint metadata.")
    private ApiExt apiExt;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1).", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
