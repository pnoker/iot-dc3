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
import io.github.pnoker.common.entity.ext.PointExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * View object for point API responses.
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
public class PointVO extends BaseVO {

    /**
     * Name
     */
    @NotBlank(message = "Point name can't be empty", groups = {Add.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid point name format",
            groups = {Add.class, Update.class})
    private String pointName;

    /**
     * Code
     */
    private String pointCode;

    /**
     * Type
     */
    @NotNull(message = "Point type can't be empty", groups = {Add.class, Update.class})
    private PointTypeFlagEnum pointTypeFlag;

    /**
     *
     */
    @NotNull(message = "Read/write flag can't be empty", groups = {Add.class, Update.class})
    private RwFlagEnum rwFlag;

    /**
     *
     */
    private BigDecimal baseValue;

    /**
     *
     */
    private BigDecimal multiple;

    /**
     *
     */
    @NotNull(message = "Value decimal can't be empty", groups = {Add.class, Update.class})
    private Byte valueDecimal;

    /**
     *
     */
    private String unit;

    /**
     * ID
     */
    @NotNull(message = "Profile ID can't be empty", groups = {Add.class, Update.class})
    private Long profileId;

    /**
     *
     */
    private PointExt pointExt;

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
