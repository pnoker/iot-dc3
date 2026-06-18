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

package io.github.pnoker.common.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.ProfileExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ProfileShareTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.valid.Add;
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
 * View object for profile API responses.
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
@Schema(description = "Profile view object")
public class ProfileVO extends BaseVO {

    /**
     * Name
     */
    @NotBlank(message = "Profile name can't be empty", groups = {Add.class})
    @Schema(description = "Profile name. Unique name within a tenant.", example = "Modbus PLC Template", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid profile name format",
            groups = {Add.class, Update.class})
    private String profileName;

    /**
     * Code
     */
    @Schema(description = "Profile code. Stable business identifier; must not change once deployed.", example = "PLC_TEMPLATE_V1")
    private String profileCode;

    /**
     * Type
     */
    @Schema(description = "Share scope of the profile: TENANT (shared within tenant), DRIVER (shared under driver), or USER (shared under user).", example = "TENANT")
    private ProfileShareTypeEnum profileShareFlag;

    /**
     * Type
     */
    @Schema(description = "Origin of the profile: SYSTEM (created by system), DRIVER (created by driver), or USER (created by user).", example = "USER")
    private ProfileTypeEnum profileTypeFlag;

    /**
     *
     */
    @Schema(description = "Profile extension information, serialized as JSON for custom configuration metadata.")
    private ProfileExt profileExt;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1).", example = "ENABLE")
    private EnableFlagEnum enableFlag;

    /**
     *
     */
    @Schema(description = "Signature used for configuration integrity verification.")
    private String signature;

    /**
     *
     */
    @Schema(description = "Optimistic-lock version number for concurrent update control.", example = "1")
    private Integer version;

}
