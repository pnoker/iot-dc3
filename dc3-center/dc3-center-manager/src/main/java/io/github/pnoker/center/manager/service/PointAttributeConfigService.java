/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.service;

import io.github.pnoker.center.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.center.manager.entity.query.PointAttributeConfigQuery;
import io.github.pnoker.common.base.service.BaseService;

import java.util.List;

/**
 * PointInfo Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface PointAttributeConfigService extends BaseService<PointAttributeConfigBO, PointAttributeConfigQuery> {

    /**
     * 根据 设备ID 查询
     *
     * @param deviceId 设备ID
     * @return PointInfo Array
     */
    List<PointAttributeConfigBO> selectByDeviceId(Long deviceId);

    /**
     * 根据 属性ID 查询
     *
     * @param attributeId 属性ID
     * @return PointInfo Array
     */
    List<PointAttributeConfigBO> selectByAttributeId(Long attributeId);

    /**
     * 根据 设备ID 和 位号ID 查询
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @return PointInfo Array
     */
    List<PointAttributeConfigBO> selectByDeviceIdAndPointId(Long deviceId, Long pointId);

    /**
     * 根据 属性ID 和 设备ID 和 位号ID 查询
     *
     * @param attributeId 属性ID
     * @param deviceId    设备ID
     * @param pointId     位号ID
     * @return PointInfo
     */
    PointAttributeConfigBO selectByAttributeIdAndDeviceIdAndPointId(Long attributeId, Long deviceId, Long pointId);
}
