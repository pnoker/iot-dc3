/*
 * Copyright 2016-present the original author or authors.
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

import io.github.pnoker.center.manager.entity.query.PointAttributeConfigPageQuery;
import io.github.pnoker.common.base.Service;
import io.github.pnoker.common.model.PointAttributeConfig;

import java.util.List;

/**
 * PointInfo Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface PointAttributeConfigService extends Service<PointAttributeConfig, PointAttributeConfigPageQuery> {

    /**
     * 根据位号配置信息 ID 、 设备 ID 、 位号 ID 查询
     *
     * @param pointAttributeId Point Attribute ID
     * @param deviceId         设备ID
     * @param pointId          Point ID
     * @return PointInfo
     */
    PointAttributeConfig selectByAttributeIdAndDeviceIdAndPointId(String pointAttributeId, String deviceId, String pointId);

    /**
     * 根据位号配置信息 ID 查询
     *
     * @param pointAttributeId Point Attribute ID
     * @return PointInfo Array
     */
    List<PointAttributeConfig> selectByAttributeId(String pointAttributeId);

    /**
     * 根据 设备 ID 查询
     *
     * @param deviceId 设备ID
     * @return PointInfo Array
     */
    List<PointAttributeConfig> selectByDeviceId(String deviceId);

    /**
     * 根据 设备 ID 、 位号 ID 查询
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @return PointInfo Array
     */
    List<PointAttributeConfig> selectByDeviceIdAndPointId(String deviceId, String pointId);
}
