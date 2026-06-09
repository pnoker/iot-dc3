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
import io.github.pnoker.common.entity.ext.MenuExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.MenuLevelEnum;
import io.github.pnoker.common.enums.MenuTypeFlagEnum;
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
 * View object for menu API responses.
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
@Schema(description = "Menu view object")
public class MenuVO extends BaseVO {

    /**
     * ID
     */
    @Schema(description = "Parent menu ID", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Menu parent id can't be empty", groups = {Add.class, Update.class})
    private Long parentMenuId;

    /**
     * Type
     */
    @Schema(description = "Menu type flag", example = "COMMON")
    private MenuTypeFlagEnum menuTypeFlag;

    /**
     * Name
     */
    @NotBlank(message = "Menu name can't be empty", groups = {Add.class, Auth.class})
    @Schema(description = "Menu name", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid menu name",
            groups = {Add.class, Update.class})
    private String menuName;

    /**
     * Code, URLMD5
     */
    @Schema(description = "Menu code")
    private String menuCode;

    /**
     *
     */
    @Schema(description = "Menu level", example = "ROOT")
    private MenuLevelEnum menuLevel;

    /**
     *
     */
    @Schema(description = "Menu display index", example = "1")
    private Integer menuIndex;

    /**
     *
     */
    @Schema(description = "Menu extension information (JSON)")
    private MenuExt menuExt;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: 0=enabled, 1=disabled", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
