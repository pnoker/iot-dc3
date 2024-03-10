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

package io.github.pnoker.center.manager.biz;

import io.github.pnoker.center.manager.entity.bo.*;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;

/**
 * Driver Notify Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface DriverNotifyService {

    /**
     * 通知驱动 新增模板(ADD) / 删除模板(DELETE) / 更新模板(UPDATE)
     *
     * @param command   Operation Type
     * @param profileBO Profile
     */
    void notifyProfile(MetadataCommandTypeEnum command, ProfileBO profileBO);

    /**
     * 通知驱动 新增位号(ADD) / 删除位号(DELETE) / 更新位号(UPDATE)
     *
     * @param command Operation Type
     * @param pointBO Point
     */
    void notifyPoint(MetadataCommandTypeEnum command, PointBO pointBO);

    /**
     * 通知驱动 新增设备(ADD) / 删除设备(DELETE) / 更新设备(UPDATE)
     *
     * @param command  Operation Type
     * @param deviceBO Device
     */
    void notifyDevice(MetadataCommandTypeEnum command, DeviceBO deviceBO);

    /**
     * 通知驱动 新增驱动配置(ADD) / 删除驱动配置(DELETE) / 更新驱动配置(UPDATE)
     *
     * @param command                 Operation Type
     * @param driverAttributeConfigBO Driver Attribute Config
     */
    void notifyDriverAttributeConfig(MetadataCommandTypeEnum command, DriverAttributeConfigBO driverAttributeConfigBO);

    /**
     * 通知驱动 新增位号配置(ADD) / 删除位号配置(DELETE) / 更新位号配置(UPDATE)
     *
     * @param command                Operation Type
     * @param pointAttributeConfigBO PointInfo
     */
    void notifyPointAttributeConfig(MetadataCommandTypeEnum command, PointAttributeConfigBO pointAttributeConfigBO);

}
