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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "driver code")
    private String driverCode;

    /**
     * AppID
     */
    @Schema(description = "driver app ID")
    private String driverAppId;

    /**
     * AppKey
     */
    @Schema(description = "driver app key")
    @ToString.Exclude
    private String driverAppKey;

    /**
     *
     */
    @Schema(description = "Token expiration flag", example = "PERMANENT")
    private ExpireTypeEnum expireFlag;

    /**
     *
     */
    @Schema(description = "expire time")
    private LocalDateTime expireTime;

    /**
     * Enable flag
     */
    @Schema(description = "Enable flag: 0=enabled, 1=disabled", example = "ENABLE")
    private EnableFlagEnum enableFlag;

}
