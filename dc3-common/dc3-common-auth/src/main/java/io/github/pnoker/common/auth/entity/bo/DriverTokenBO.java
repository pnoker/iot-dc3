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

package io.github.pnoker.common.auth.entity.bo;

import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ExpireFlagEnum;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DriverToken BO
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class DriverTokenBO extends BaseBO {

    /**
     * 驱动编号
     */
    private String driverCode;

    /**
     * 驱动AppID
     */
    private String driverAppId;

    /**
     * 驱动AppKey
     */
    private String driverAppKey;

    /**
     * 失效标识
     */
    private ExpireFlagEnum expireFlag;

    /**
     * 失效时间
     */
    private LocalDateTime expireTime;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
