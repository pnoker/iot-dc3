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

package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.base.service.BaseService;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.entity.query.DriverAttributeConfigQuery;

import java.util.List;

/**
 * DriverConfig Interface
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DriverAttributeConfigService extends BaseService<DriverAttributeConfigBO, DriverAttributeConfigQuery> {

    /**
     * Device ID
     *
     * @param deviceId Device ID
     * @return DriverConfig
     */
    List<DriverAttributeConfigBO> selectByDeviceId(Long deviceId);

    /**
     * ID
     *
     * @param attributeId ID
     * @return DriverConfig
     */
    List<DriverAttributeConfigBO> selectByAttributeId(Long attributeId);

    /**
     * ID Device ID
     *
     * @param deviceId    Device ID
     * @param attributeId ID
     * @return DriverConfig
     */
    DriverAttributeConfigBO selectByAttributeIdAndDeviceId(Long deviceId, Long attributeId);

    /**
     * @param entityBO {@link DriverAttributeConfigBO}
     * @return {@link DeviceBO}
     */
    DriverAttributeConfigBO innerSave(DriverAttributeConfigBO entityBO);

}
