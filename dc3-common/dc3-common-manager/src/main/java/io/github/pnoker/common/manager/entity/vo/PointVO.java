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
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Point view object")
public class PointVO extends BaseVO {

    /**
     * Name
     */
    @NotBlank(message = "Point name can't be empty", groups = {Add.class})
    @Schema(description = "Data point name. Unique name within a tenant for identifying a specific measurable or controllable device attribute.", example = "Temperature", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid point name format",
            groups = {Add.class, Update.class})
    private String pointName;

    /**
     * Code
     */
    @Schema(description = "Data point code. Stable business identifier; must not change once deployed.", example = "TEMP_001")
    private String pointCode;

    /**
     * Type
     */
    @Schema(description = "Value data type held by this point (e.g. STRING, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN).", example = "STRING", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Point type can't be empty", groups = {Add.class, Update.class})
    private PointTypeEnum pointTypeFlag;

    /**
     *
     */
    @Schema(description = "Read/write capability: READ_ONLY, WRITE_ONLY, or READ_WRITE.", example = "READ_ONLY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Read/write flag can't be empty", groups = {Add.class, Update.class})
    private RwTypeEnum rwFlag;

    /**
     *
     */
    @Schema(description = "Base value applied before multiplier for raw-to-engineering-unit conversion.", example = "0.0")
    private BigDecimal baseValue;

    /**
     *
     */
    @Schema(description = "Multiplier for raw-to-engineering-unit conversion: engineeringValue = baseValue + rawValue * multiplier.", example = "1.0")
    private BigDecimal multiple;

    /**
     *
     */
    @Schema(description = "Number of decimal places to retain when formatting the calculated value.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Value decimal can't be empty", groups = {Add.class, Update.class})
    private Byte valueDecimal;

    /**
     *
     */
    @Schema(description = "Engineering unit for the point value (e.g. °C, kPa, rpm).", example = "°C")
    private String unit;

    /**
     * ID
     */
    @Schema(description = "ID of the profile (device template) this data point belongs to.", example = "2048", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Profile ID can't be empty", groups = {Add.class, Update.class})
    private Long profileId;

    /**
     *
     */
    @Schema(description = "Data point extension information, serialized as JSON for custom attributes and metadata.")
    private PointExt pointExt;

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
