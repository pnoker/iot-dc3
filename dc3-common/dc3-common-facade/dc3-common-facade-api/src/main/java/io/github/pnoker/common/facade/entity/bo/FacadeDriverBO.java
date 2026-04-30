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
import io.github.pnoker.common.entity.ext.DriverExt;
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.*;

/**
 * Facade-level driver BO. Field set matches {@code api.center.manager.DriverApi}.
 *
 * @author pnoker
 * @since 2026.4.30
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class FacadeDriverBO extends BaseBO {

    private String driverName;

    private String driverCode;

    private String serviceName;

    private String serviceHost;

    private DriverTypeFlagEnum driverTypeFlag;

    private DriverExt driverExt;

    private EnableFlagEnum enableFlag;

    private Long tenantId;

    private String signature;

    private Integer version;
}
