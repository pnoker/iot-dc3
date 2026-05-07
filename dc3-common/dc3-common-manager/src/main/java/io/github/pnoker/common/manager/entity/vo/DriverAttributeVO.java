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
import io.github.pnoker.common.entity.ext.DriverAttributeExt;
import io.github.pnoker.common.enums.AttributeTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * VO
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DriverAttributeVO extends BaseVO {

    /**
     * Name
     */
    @NotBlank(message = "属性名称不能为空", groups = {Add.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "属性名称格式无效",
            groups = {Add.class, Update.class})
    private String attributeName;

    /**
     * Code
     */
    @NotBlank(message = "属性编号不能为空", groups = {Add.class})
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "属性编号格式无效",
            groups = {Add.class, Update.class})
    private String attributeCode;

    /**
     * Type
     */
    private AttributeTypeFlagEnum attributeTypeFlag;

    /**
     *
     */
    private String defaultValue;

    /**
     * Driver ID
     */
    @NotNull(message = "驱动ID不能为空", groups = {Add.class, Update.class})
    private Long driverId;

    /**
     *
     */
    private DriverAttributeExt attributeExt;

    /**
     * Enable flag
     */
    private EnableFlagEnum enableFlag;

    /**
     *
     */
    private String signature;

    /**
     *
     */
    private Integer version;

}
