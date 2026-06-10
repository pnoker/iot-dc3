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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "Service name")
    private String serviceName;

    /**
     * ApiType
     */
    @Schema(description = "API method enum", example = "GET")
    private ApiTypeEnum apiTypeFlag;

    /**
     * ApiName
     */
    @NotBlank(message = "API name can't be empty", groups = {Add.class, Auth.class})
    @Schema(description = "API name", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid API name", groups = {Add.class, Update.class})
    private String apiName;

    /**
     * ApiCode, URLMD5
     */
    @Schema(description = "API code identifier")
    private String apiCode;

    /**
     * API grouping, usually the owning controller simple class name
     */
    @Schema(description = "API grouping label")
    private String apiGroup;

    /**
     * Api
     */
    @Schema(description = "api extension information in JSON format")
    private ApiExt apiExt;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag enum (ENABLE or DISABLE)", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
