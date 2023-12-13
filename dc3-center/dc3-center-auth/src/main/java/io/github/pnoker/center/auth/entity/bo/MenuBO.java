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

package io.github.pnoker.center.auth.entity.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.github.pnoker.center.auth.entity.bo.ext.MenuExtBO;
import io.github.pnoker.common.base.BaseBO;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.constant.enums.MenuTypeFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Auth;
import io.github.pnoker.common.valid.Update;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Menu BO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MenuBO extends BaseBO {

    /**
     * 菜单父级ID
     */
    @NotBlank(message = "Menu parent id can't be empty",
            groups = {Add.class, Update.class})
    private String parentMenuId;

    /**
     * 菜单类型标识
     */
    private MenuTypeFlagEnum menuTypeFlag;

    /**
     * 菜单名称
     */
    @NotBlank(message = "Menu name can't be empty",
            groups = {Add.class, Auth.class})
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$",
            message = "Invalid menu name",
            groups = {Add.class, Update.class})
    private String menuName;

    /**
     * 菜单编号，一般为URL的MD5编码
     */
    private String menuCode;

    /**
     * 菜单层级
     */
    private Integer menuLevel;

    /**
     * 菜单顺序
     */
    private Integer menuIndex;

    /**
     * 菜单拓展信息
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private MenuExtBO menuExtBO;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
