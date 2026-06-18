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
import io.github.pnoker.common.enums.AttributeTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
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

/**
 * View object for driver attribute API responses.
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
@Schema(description = "Driver Attribute view object")
public class DriverAttributeVO extends BaseVO {

    /**
     * Name
     */
    @NotBlank(message = "Attribute name can't be empty", groups = {Add.class})
    @Schema(description = "Driver attribute name. Unique name within a driver for identifying a configurable driver property.", example = "Connection Timeout", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid attribute name format",
            groups = {Add.class, Update.class})
    private String attributeName;

    /**
     * Code
     */
    @NotBlank(message = "Attribute code can't be empty", groups = {Add.class})
    @Schema(description = "Driver attribute code. Stable business identifier; must not change once deployed.", example = "CONN_TIMEOUT", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid attribute code format",
            groups = {Add.class, Update.class})
    private String attributeCode;

    /**
     * Type
     */
    @Schema(description = "Driver attribute data type: STRING, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, or BOOLEAN.", example = "INT")
    private AttributeTypeEnum attributeTypeFlag;

    /**
     *
     */
    @Schema(description = "Default attribute value when no per-device configuration is provided.", example = "5000")
    private String defaultValue;

    /**
     * Driver ID
     */
    @Schema(description = "ID of the protocol driver this attribute belongs to.", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Driver ID can't be empty", groups = {Add.class, Update.class})
    private Long driverId;

    /**
     *
     */
    @Schema(description = "Driver attribute extension information, serialized as JSON for custom metadata.")
    private DriverAttributeExt attributeExt;

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
