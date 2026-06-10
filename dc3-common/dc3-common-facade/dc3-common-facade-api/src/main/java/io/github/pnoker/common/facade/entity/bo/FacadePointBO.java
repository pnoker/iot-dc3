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

package io.github.pnoker.common.facade.entity.bo;

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
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Facade-level point BO. Field set matches {@code api.center.manager.PointApi}.
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
@Schema(description = "Facade Point business object")
public class FacadePointBO extends BaseBO {

    @Schema(description = "point name")

    private String pointName;

    @Schema(description = "point code")

    private String pointCode;

    @Schema(description = "Point type enum")

    private PointTypeEnum pointTypeFlag;

    @Schema(description = "Read/write type enum")

    private RwTypeEnum rwFlag;

    @Schema(description = "base value")

    private BigDecimal baseValue;

    @Schema(description = "Value multiplier")

    private BigDecimal multiple;

    @Schema(description = "value decimal")

    private Byte valueDecimal;

    @Schema(description = "Point value unit")

    private String unit;

    @Schema(description = "profile ID")

    private Long profileId;

    @Schema(description = "point extension information in JSON format")

    private PointExt pointExt;

    @Schema(description = "Enable flag enum (ENABLE or DISABLE)")

    private EnableFlagEnum enableFlag;

    @Schema(description = "Tenant ID")

    private Long tenantId;

    @Schema(description = "Configuration signature")

    private String signature;

    @Schema(description = "Version number")

    private Integer version;

}
