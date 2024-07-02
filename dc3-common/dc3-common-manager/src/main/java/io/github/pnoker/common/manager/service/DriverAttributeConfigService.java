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
