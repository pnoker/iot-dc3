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

import io.github.pnoker.common.driver.entity.dto.CommandAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.EventAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeConfigDTO;
import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.ext.DeviceExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
import java.util.Set;

/**
 * Business object that describes a device assigned to the current driver, including the
 * cached point list and resolved attribute configurations.
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
public class DeviceBO extends BaseBO {

    /**
     * Device display name.
     */
    private String deviceName;

    /**
     * Device code defined in the platform.
     */
    private String deviceCode;

    /**
     * Owning driver identifier.
     */
    private Long driverId;

    /**
     * Extended device metadata.
     */
    private DeviceExt deviceExt;

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

    /**
     * Assigned profile identifier.
     */
    private Long profileId;

    /**
     * Identifiers of points owned by the device.
     */
    private Set<Long> pointIds;

    /**
     * Driver attribute configuration map keyed by attribute identifier.
     */
    private Map<Long, DriverAttributeConfigDTO> driverAttributeConfigIdMap;

    /**
     * Point attribute configuration map keyed by point identifier and then attribute
     * identifier.
     */
    private Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigIdMap;

    /**
     * Command attribute configuration map keyed by command identifier and then
     * attribute identifier.
     */
    private Map<Long, Map<Long, CommandAttributeConfigDTO>> commandAttributeConfigIdMap;

    /**
     * Event attribute configuration map keyed by event identifier and then attribute
     * identifier.
     */
    private Map<Long, Map<Long, EventAttributeConfigDTO>> eventAttributeConfigIdMap;

}
