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
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for driver attribute configuration API responses.
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
@Schema(description = "Driver Attribute Config view object")
public class DriverAttributeConfigVO extends BaseVO {

    /**
     * ID
     */
    @Schema(description = "ID of the driver attribute whose value is being configured.", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Driver attribute ID can't be empty", groups = {Add.class, Update.class})
    private Long attributeId;

    /**
     *
     */
    @Schema(description = "The configured value overriding the attribute default for this specific device.", example = "5000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Driver attribute config value can't be empty")
    private String configValue;

    /**
     * Device ID
     */
    @Schema(description = "ID of the device this configuration applies to.", example = "2048", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Device ID can't be empty", groups = {Add.class, Update.class})
    private Long deviceId;

    /**
     *
     */
    @Schema(description = "Configuration extension information, serialized as JSON for custom metadata.")
    private JsonExt configExt;

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
