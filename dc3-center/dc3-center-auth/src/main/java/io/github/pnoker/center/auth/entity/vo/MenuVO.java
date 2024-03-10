/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.MenuExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.MenuLevelFlagEnum;
import io.github.pnoker.common.enums.MenuTypeFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Auth;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Menu VO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "Menu", description = "菜单")
public class MenuVO extends BaseVO {

    /**
     * 菜单父级ID
     */
    @Schema(description = "菜单父级ID")
    @NotBlank(message = "Menu parent id can't be empty",
            groups = {Add.class, Update.class})
    private Long parentMenuId;

    /**
     * 菜单类型标识
     */
    @Schema(description = "菜单类型标识")
    private MenuTypeFlagEnum menuTypeFlag;

    /**
     * 菜单名称
     */
    @Schema(description = "菜单名称")
    @NotBlank(message = "Menu name can't be empty",
            groups = {Add.class, Auth.class})
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$",
            message = "Invalid menu name",
            groups = {Add.class, Update.class})
    private String menuName;

    /**
     * 菜单编号，一般为URL的MD5编码
     */
    @Schema(description = "菜单编号")
    private String menuCode;

    /**
     * 菜单层级
     */
    @Schema(description = "菜单层级")
    private MenuLevelFlagEnum menuLevel;

    /**
     * 菜单顺序
     */
    @Schema(description = "菜单顺序")
    private Integer menuIndex;

    /**
     * 菜单拓展信息
     */
    @Schema(description = "菜单拓展信息")
    private MenuExt menuExt;

    /**
     * 使能标识
     */
    @Schema(description = "使能标识")
    private EnableFlagEnum enableFlag;
}
