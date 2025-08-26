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
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface DriverAttributeConfigService extends BaseService<DriverAttributeConfigBO, DriverAttributeConfigQuery> {

    /**
     * 根据 设备ID 查询
     *
     * @param deviceId 设备ID
     * @return DriverConfig 集合
     */
    List<DriverAttributeConfigBO> selectByDeviceId(Long deviceId);

    /**
     * 根据 驱动属性配置ID 查询
     *
     * @param attributeId 驱动属性ID
     * @return DriverConfig 集合
     */
    List<DriverAttributeConfigBO> selectByAttributeId(Long attributeId);

    /**
     * 根据 驱动属性配置ID 和 设备ID 查询
     *
     * @param deviceId    设备ID
     * @param attributeId 驱动属性ID
     * @return DriverConfig
     */
    DriverAttributeConfigBO selectByAttributeIdAndDeviceId(Long deviceId, Long attributeId);

    /**
     * 内部保存
     *
     * @param entityBO {@link DriverAttributeConfigBO}
     * @return {@link DeviceBO}
     */
    DriverAttributeConfigBO innerSave(DriverAttributeConfigBO entityBO);
}
