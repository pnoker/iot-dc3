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
import io.github.pnoker.common.entity.ext.DeviceExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.common.valid.Upload;
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
 * View object for device API responses.
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
@Schema(description = "Device view object")
public class DeviceVO extends BaseVO {

    /**
     * Device Name
     */
    @NotBlank(message = "Device name can't be empty", groups = {Add.class})
    @Schema(description = "Device name. Unique within a tenant.", example = "Temperature Sensor 01",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$", message = "Invalid device name format",
            groups = {Add.class, Update.class})
    private String deviceName;

    /**
     * Device ID
     */
    @Schema(description = "Device code. Stable business identifier for the device.", example = "DEV_0001")
    private String deviceCode;

    /**
     * Driver ID
     */
    @Schema(description = "ID of the driver this device connects through.", example = "1024",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Driver ID can't be empty", groups = {Add.class, Update.class, Upload.class})
    private Long driverId;

    /**
     *
     */
    @Schema(description = "Device extension information, serialized as JSON.")
    private DeviceExt deviceExt;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1).", example = "ENABLE")
    private EnableFlagEnum enableFlag;

    /**
     *
     */
    @Schema(description = "Signature used for integrity verification.")
    private String signature;

    /**
     *
     */
    @Schema(description = "Optimistic-lock version number.", example = "1")
    private Integer version;

    /**
     * Profile ID
     */
    @Schema(description = "ID of the profile (device template) this device is derived from.", example = "2048",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Profile ID can't be empty", groups = {Add.class, Upload.class})
    private Long profileId;

}
