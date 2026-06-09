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

package io.github.pnoker.common.driver.entity.bo;

import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.ext.PointExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Business object that describes a device point, including read/write access and value
 * calculation parameters.
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
public class PointBO extends BaseBO {

    /**
     * Point display name.
     */
    private String pointName;

    /**
     * Point code defined in the platform.
     */
    private String pointCode;

    /**
     * Point data type.
     */
    private PointTypeEnum pointTypeFlag;

    /**
     * Read/write capability of the point.
     */
    private RwTypeEnum rwFlag;

    /**
     * Base offset applied during value conversion.
     */
    private BigDecimal baseValue;

    /**
     * Multiplier applied during value conversion.
     */
    private BigDecimal multiple;

    /**
     * Decimal precision used for floating-point rounding.
     */
    private Byte valueDecimal;

    /**
     * Engineering unit of the point value.
     */
    private String unit;

    /**
     * Associated profile identifier.
     */
    private Long profileId;

    /**
     * Extended point metadata.
     */
    private PointExt pointExt;

    /**
     * Enable flag
     */
    private EnableFlagEnum enableFlag;

    /**
     * Tenant identifier.
     */
    private Long tenantId;

    /**
     * Data signature used for optimistic checks or synchronization.
     */
    private String signature;

    /**
     * Data version.
     */
    private Integer version;

}
