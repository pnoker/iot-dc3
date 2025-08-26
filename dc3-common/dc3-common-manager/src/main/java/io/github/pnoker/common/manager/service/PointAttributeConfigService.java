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
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.query.PointAttributeConfigQuery;

import java.util.List;

/**
 * PointConfig Interface
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface PointAttributeConfigService extends BaseService<PointAttributeConfigBO, PointAttributeConfigQuery> {

    /**
     * 根据 设备ID 查询
     *
     * @param deviceId 设备ID
     * @return PointConfig 集合
     */
    List<PointAttributeConfigBO> selectByDeviceId(Long deviceId);

    /**
     * 根据 属性ID 查询
     *
     * @param attributeId 属性ID
     * @return PointConfig 集合
     */
    List<PointAttributeConfigBO> selectByAttributeId(Long attributeId);

    /**
     * 根据 设备ID 和 位号ID 查询
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @return PointConfig 集合
     */
    List<PointAttributeConfigBO> selectByDeviceIdAndPointId(Long deviceId, Long pointId);

    /**
     * 根据 属性ID 和 设备ID 和 位号ID 查询
     *
     * @param attributeId 属性ID
     * @param deviceId    设备ID
     * @param pointId     位号ID
     * @return PointConfig
     */
    PointAttributeConfigBO selectByAttributeIdAndDeviceIdAndPointId(Long attributeId, Long deviceId, Long pointId);

    /**
     * 内部保存
     *
     * @param entityBO {@link PointAttributeConfigBO}
     * @return {@link DeviceBO}
     */
    PointAttributeConfigBO innerSave(PointAttributeConfigBO entityBO);
}
