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

package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ExpireTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * View object for driver token API responses.
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
@Schema(description = "Driver Token view object")
public class DriverTokenVO extends BaseVO {

    /**
     * Driver ID
     */
    @Schema(description = "Driver code. Stable routing identifier for the driver this token authenticates.", example = "dc3-driver-modbus-tcp")
    private String driverCode;

    /**
     * AppID
     */
    @Schema(description = "Application ID issued to the driver during registration; used as the client identifier in token exchange.", example = "app_abc123")
    private String driverAppId;

    /**
     * AppKey
     */
    @Schema(description = "Application key (secret) issued to the driver; used as the client secret in token exchange.", example = "sk-xxxx")
    @ToString.Exclude
    private String driverAppKey;

    /**
     *
     */
    @Schema(description = "Expiration policy applied to this driver token; determines how long the token remains valid before requiring renewal.", example = "PERMANENT")
    private ExpireTypeEnum expireFlag;

    /**
     *
     */
    @Schema(description = "Token expiration timestamp. After this time the token is invalid and must be refreshed.", example = "2026-12-31T23:59:59")
    private LocalDateTime expireTime;

    /**
     * Enable flag
     */
    @Schema(description = "Whether this driver token is active; disabled tokens are rejected during authentication.", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
