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
import io.github.pnoker.common.entity.ext.RoleExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Auth;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * View object for role API responses.
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
@Schema(description = "Role view object")
public class RoleVO extends BaseVO {

    /**
     * ID
     */
    @Schema(description = "Parent role ID", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Role parent id can't be empty", groups = {Add.class, Update.class})
    private Long parentRoleId;

    /**
     * Name
     */
    @NotBlank(message = "Role name can't be empty", groups = {Add.class, Auth.class})
    @Schema(description = "Role name", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid role name",
            groups = {Add.class, Update.class})
    private String roleName;

    /**
     * Code
     */
    @Schema(description = "Role code")
    private String roleCode;

    /**
     *
     */
    @Schema(description = "Role extension information in JSON format")
    private RoleExt roleExt;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag enum (ENABLE or DISABLE)", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
